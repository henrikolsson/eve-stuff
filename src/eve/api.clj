(ns eve.api
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn get-industry-systems []
  (json/read-str (:body @(http/get
                          "http://public-crest.eveonline.com/industry/systems/"))))

(defn get-adjusted-prices []
  (json/read-str (:body @(http/get
                          "http://public-crest.eveonline.com/market/prices/"))))

(defn get-market-stat [types]
  (let [url "http://api.eve-central.com/api/marketstat/json"
        data (str (reduce (fn [one two]
                            (str one "typeid=" two "&"))
                          "" types)
                  "usesystem=30000142")]
    (json/read-str (:body @(http/post url
                                {:body data
                                 :headers {"Content-Type"
                                           "application/x-www-form-urlencoded"}})))))




