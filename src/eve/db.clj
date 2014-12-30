(ns eve.db
  (:import [java.sql Timestamp]
           [java.util Date])
  (:use [korma.core]
        [korma.db])
  (:require [taoensso.timbre :as timbre]
            [clojure.core.memoize :as memo]))

(defdb evedb (postgres {:db "evedb"
                        :user "eve"
                        :password "eve"
                        :delimiters ""}))

(defn get-system-by-name [name]
  (first (select "mapSolarSystems"
                 (where {:solarSystemName name}))))

(defn get-system-by-id [id]
  (first (select "mapSolarSystems"
                 (where {:solarSystemID id}))))

(defn get-neighbours [solarSystemID]
  (map #(get %1 :toSolarSystemID)
       (select "mapSolarSystemJumps"
               (where {:fromSolarSystemID solarSystemID}))))

(def get-neighbours-memoized
  (memoize get-neighbours))

(defn activity-id-to-keyword [id]
  (condp = id
    8 :invention
    1 :manufacturing
    3 :research_time
    4 :research_material
    5 :copying))

(defn activity-keyword-to-id [kw]
  (condp = kw
    :invention 8
    :manufacturing 1
    :research_time 3
    :research_material 4
    :copying 5))

(defn get-cost-index [id]
  (first (select "systemCostIndex"
                 (where {:id id}))))

(defn update-price [row]
  (if (> (count (select "price"
                        (where {:id (:id row)}))) 0)
    (update "price"
            (set-fields row)
            (where {:id (:id row)}))
    (insert "price"
            (values row))))

(defn get-price [typeid]
  (first (select "price"
                 (where {:id typeid}))))

(defn update-system-cost-index [row]
  (if (> (count (select "systemCostIndex"
                        (where {:id (:id row)}))) 0)
    (update "systemCostIndex"
            (set-fields row)
            (where {:id (:id row)}))
    (insert "systemCostIndex"
            (values row))))

(defn update-adjusted-price [row]
  (if (> (count (select "adjustedPrice"
                        (where {:id (:id row)}))) 0)
    (update "adjustedPrice"
            (set-fields row)
            (where {:id (:id row)}))
    (insert "adjustedPrice"
            (values row))))

(defn get-adjusted-price [id]
  (first (select "adjustedprice"
                 (where {:id id}))))

(defn get-market-type-ids []
  (map #(:typeid %1)
       (select "invTypes"
               (fields :typeid)
               (where {:marketgroupid [< 350000]}))))

(defn get-item-by-id [id]
  (first (select "invTypes"
                 (where {:typeID id}))))

(defn get-item-by-name [name]
  (first (select "invTypes"
                 (where {:typeName name}))))

(defn get-materials [bpid me runs]
  (map (fn [row]
         (let [quantity (:quantity row)]
           (assoc row :adjustedquantity
                  (int (Math/ceil
                        (/
                         (Math/round
                          (* (* runs (float (* (- 1 (/ me 100)) quantity))) 100.0))
                         100.0))))))
       (select "industryActivityMaterials"
          (where {:typeID bpid
                  :activityid 1}))))

(defn get-install-price [typeid index runs]
  (let [adjusted-price (:adjustedprice (get-adjusted-price typeid))]
    (* (* adjusted-price index) runs)))

(defn get-system-tax [price]
  (* price 0.10))

(defn get-blueprint-for-item [id]
  (first (select "industryActivityProducts"
                 (where {:productTypeID id
                         :activityid 1}))))

(defn get-manufacturing-cost [item-name system me runs]
  (let [item (get-item-by-name item-name)
        blueprint (get-blueprint-for-item (:typeid item))
        index (:manufacturing
               (get-cost-index (:solarsystemid (get-system-by-name system))))
        install (get-install-price (:typeid item) index runs)
        tax (* install 0.10)
        materialcost (reduce (fn [acc row]
                               (let [mat (:materialtypeid row)
                                     quant (:adjustedquantity row)
                                     buy (:buy (get-price mat))
                                     cost (* buy quant)]
                                 (+ acc cost)))
                             0 (get-materials (:typeid blueprint) me runs))]
    {:install (+ tax install)
     :materials materialcost}))

(defn get-base-invention-chance [item-id]
  (let [item (get-item-by-id item-id)
        meta (first (select "invMetaTypes"
                            (where {:parenttypeid item-id
                                    :metagroupid 2})))
        base (condp = (:groupid item)
               513 0.18
               27 0.22
               26 0.26
               419 0.26
               463 0.26
               28 0.26
               25 0.30
               420 0.30
               nil)]
    (if base
      base
      (if meta
        0.40
        0))))

(defn get-invention-chance [base-item-id science1 science2 racial]
  (let [base (get-base-invention-chance base-item-id)]
    (* base
       (+ (/ (+ science1 science2) 30.0)
          (/ racial 40.0)
          1))))

(defn get-profit [item-name system me runs]
  (let [cost (get-manufacturing-cost item-name system me runs)
        sell (* (:sell (get-price (:typeid (get-item-by-name item-name))))
                runs)]
    (- (* sell (- 1.0 0.014))
       (* (:materials cost) 1.0065))))
