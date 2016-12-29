(ns xdotool.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [clojure.set :as set]
            ))

(defn run [command & args]
  (println "cmd" command "args" args)
  (apply sh "xdotool" command (map str args)))

(defn- parse-int [string] (-> string str/trim Integer/parseInt))
(defn- parse-pair [position splitter ks]
  (as-> position _
    (str/split _ splitter)
    (map #(Integer/parseInt %) _)
    (zipmap ks _)
    ))

(def no-args-options #{:clearmodifiers :sync :polar :class :classname 
                       :name :onlyvisible :title :all :any :usehints
                       :relative
                       })

(defn- prepare-options [opt-map arg-opt-exceptions]
  (->> opt-map
       (map (fn [[k v]]
              (if ((set/difference no-args-options arg-opt-exceptions) k)
                (str "--" (name k))
                (str "--" (name k) " " v))))
       (str/join " ")))

(defn- map-options-on-arg-list [args valid-options arg-opt-exceptions]
  (cons (prepare-options (select-keys (first args) valid-options) arg-opt-exceptions) (rest args)))

(defmacro defcommand [command-name {:keys [parser valid-options command no-args-exceptions] 
                                    :or {valid-options #{}
                                         parser identity
                                         no-args-exceptions #{}
                                         }}]
  (let [xdo-command (or command (-> command-name str (str/replace "-" "")))
        has-options (not (empty? valid-options))
        args (gensym)]
    ;; TODO: select keys with options
    `(defn ~command-name [& ~args]
       (-> (apply run ~xdo-command (if (and ~has-options (map? (first ~args)))
                                     (map-options-on-arg-list ~args ~valid-options ~no-args-exceptions)
                                     ~args))
           :out
           str/trim-newline
           (~parser)))
    ))

;; keyboard funcs
(defcommand key {:valid-options #{:clearmodifiers :window :delay}})
(defcommand key-down {:valid-options #{:clearmodifiers :window :delay}})
(defcommand key-up {})
(defcommand type {:valid-options #{:clearmodifiers :window :delay}})

;; mouse funcs
(defcommand mouse-move {:valid-options #{:window :screen :polar :clearmodifiers :sync}})
(defcommand mouse-move-relative {:valid-options #{:polar :sync :clearmodifiers}
                                 :command "mousemove_relative"})
(defcommand click {:valid-options #{:clearmodifiers :repeat :delay :window}})
(defcommand mouse-down {:valid-options #{:clearmodifiers :repeat :delay :window}})
(defcommand mouse-up {:valid-options #{:clearmodifiers :repeat :delay :window}})
(defcommand get-mouse-location 
  {:parser (fn [input]
             (as-> input _
               (str/trim _)
               (str/split _ #" ")
               (map #(str/split % #":") _)
               (map second _)
               (map parse-int _)
               (zipmap [:x :y :screen :window] _)
               ))})

;; TODO (defcommand behave-screen-edge {:command "behave_screen_edge"})

;; window funcs
(defcommand search {:parser #(if (empty? %) [] (as-> % _ (str/trim _) (str/split _ #"\n") (map parse-int _)))
                    :valid-options #{:class :classname :maxdepth :name :onlyvisible :pid :screen :desktop
                                     :limit :title :all :any :sync}})
(defcommand select-window {:parser parse-int})
;; TODO (defcommand behave {})
(defcommand get-window-pid {:parser parse-int})
(defcommand get-window-name {})
(defcommand get-window-geometry 
  {:parser (fn [input]
             (as-> input _
               (str/split _ #"\n")
               (map str/trim _)
               (map #(str/split % #" ") _)
               (map second _)
               (zipmap [:window :position :geometry] _)
               (update _ :position #(parse-pair % #"," [:x :y]))
               (update _ :geometry #(parse-pair % #"x" [:width :height]))
               ))})
(defcommand get-window-focus {:parser parse-int})
(defcommand window-size {:valid-options #{:sync :usehints}})
(defcommand window-move {:valid-options #{:sync :relative}})
(defcommand window-focus {:valid-options #{:sync}})
(defcommand window-map {:valid-options #{:sync}})
(defcommand window-minimize {:valid-options #{:sync}})
(defcommand window-raise {})
(defcommand window-reparent {})
(defcommand window-close {})
(defcommand window-kill {})
(defcommand window-unmap {:valid-options #{:sync}})
(defcommand set-window {:command "set_window"
                        :valid-options #{:name :icon-name :role :classname :class :overrideredirect}
                        :no-args-exceptions #{:name :class :classname}
                        })

;; desktop and window commands
(defcommand window-activate {:valid-options #{:sync}})
(defcommand get-active-window {:parser parse-int})
(defcommand set-num-desktops {:command "set_num_desktops"})
(defcommand get-num-desktops {:command "get_num_desktops" :parser parse-int})
(defcommand get-desktop-viewport {:command "get_desktop_viewport"
                                  :parser #(parse-pair % #" " [:x :y])})
(defcommand set-desktop-viewport {:command "set_desktop_viewport"})
(defcommand set-desktop {:command "set_desktop"
                         :valid-options #{:relative}})
(defcommand get-desktop {:command "get_desktop"
                         :parser parse-int})
(defcommand set-desktop-for-window {:command "set_desktop_for_window"})
(defcommand get-desktop-for-window {:command ""})

