(ns eve.core
  (:require [eve.db :as db]
            [eve.api :as api]
            [clojure.tools.logging :as log]))

(defn update-system-indices []
  (let [json (api/get-industry-systems)]
    (doseq [item (get json "items")]
      (let [row
            (into {:id (get-in item ["solarSystem" "id"])}
                  (map (fn [index]
                         [(db/activity-id-to-keyword (get index "activityID"))
                          (get index "costIndex")])
                       (get item "systemCostIndices")))]
        (db/update-system-cost-index row)))))

(defn update-prices []
  (let [data (api/get-market-stat (db/get-market-type-ids))]
    (doseq [row (map (fn [item]
                        {:buy (get-in item ["buy" "max"])
                         :sell (get-in item ["sell" "min"])
                         :id (first (get-in item ["all" "forQuery" "types"]))})
                      data)]
      (db/update-price row))))

(defn update-adjusted-prices []
  (let [json (api/get-adjusted-prices)]
    (doseq [item (get json "items")]
      (let [row {:id (get-in item ["type" "id"])
                 :adjustedprice (get item "adjustedPrice")
                 :averageprice (get item "averagePrice")}]
        (db/update-adjusted-price row)))))

(comment
  (update-system-indices)
  (update-adjusted-prices)
  (update-prices))
