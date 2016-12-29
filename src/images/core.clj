(ns images.core
  (:import [org.opencv.highgui Highgui]
           [org.opencv.imgproc Imgproc]
           [org.opencv.core Core Mat CvType Point]
           ))

(defn- decode-index [index ^Mat mat]
  (let [y (quot index (.cols mat))
        x (rem index (.cols mat)) ]
    {:x x
     :y y}))

(defn search-image [^Mat image ^Mat template]
  (let [cols (- (.cols image) (.cols template) -1)
        rows (- (.rows image) (.rows template) -1)
        result (doto (Mat.) (.create rows cols CvType/CV_32FC1))
        _ (Imgproc/matchTemplate image template result 3)
        buff (make-array Float/TYPE (* (.total result) (.channels result)) )
        ]
    (.get result 0 0 buff)
    (as-> buff _
      (map-indexed #(zipmap [:index :value] [%1 %2]) _)
      (filter #(> (:value %) 0.95) _)
      (map #(update-in % [:index] decode-index result) _)
      )))

(defn load-image [image-path] (Highgui/imread image-path))

(comment

  (def lena (Highgui/imread "/home/morti/lena.jpg"))
  (def small (Highgui/imread "/home/morti/small_lena.jpg"))

  (def index 30022)

  (decode-index index result)
  (search-image lena small)

  (def cols (- (.cols lena) (.cols small) -1))
  (def rows (- (.rows lena) (.rows small) -1))

  (def result (doto (Mat.) (.create rows cols CvType/CV_32FC1)))
  (Imgproc/matchTemplate lena small result 3)
  (def buff (make-array Float/TYPE 3))
  (def r (.get result 0 0 buff))
  (get buff 0)


  (def minMax (Core/minMaxLoc result))

  (.minVal minMax)
  (.maxVal minMax)
  

  (def match (.maxLoc minMax))
  
 )
