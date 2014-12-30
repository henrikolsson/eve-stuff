(ns eve.navigation
  (:use [clojure.data.priority-map]
        [clojure.pprint])
  (:require [eve.db :as db]))

(defn reverse-path [prevs end]
  (if-not (contains? prevs end)
    nil
    (reverse
     (remove nil? (reduce (fn [acc val]
                            (conj acc (get prevs (last acc))))
                          [end]
                          prevs)))))
    
(defn get-shortest-path [start end custom-filter]
  (loop [distances (priority-map start 0)
         spt {}
         prevs {start nil}]
    (if (empty? distances)
      nil
      (let [current (first (peek distances))
            distance (second (peek distances))
            children (db/get-neighbours-memoized current)]
        (if (or (= current end)
                (> distance 30))
          (reverse-path prevs end)
          (recur (into (pop distances)
                       (map (fn [item]
                              [item (min (+ distance 1)
                                         (get distances item (+ distance 1)))])
                            (filter #(and (not (contains? spt %1))
                                          (custom-filter %1))
                                    children)))
                 (assoc spt current distance)
                 (into prevs (map (fn [item]
                                    (if-not (contains? prevs item)
                                      [item current]))
                                  children))))))))


(defn highsec-filter [id]
  (> (:security (db/get-system-by-id id)) 0.5))

;; (def with-jumps
;;   (pmap #(hash-map :id %1
;;                    :jumps
;;                    (get-shortest-path %1
;;                                       30000142
;;                                       highsec-filter))
;;        db/systems))

;; (def with-jumps-filtered
;;   (filter #(and (not (nil? (:jumps %1)))
;;                 (< (count (:jumps %1)) 20))
;;           with-jumps))

;; (count with-jumps-filtered)

;; (def with-jumps-sorted (sort-by #(count (:jumps %1)) with-jumps-filtered))

;; (pprint (map (fn [item]
;;        (let [sys (db/get-system-by-id (:id item))
;;              cost (db/get-cost-index (:id item))]
;;          {:jumps (count (:jumps item))
;;           :name (:solarSystemName sys)
;;           :manufacturing (:manufacturing cost)
;;           :invention (:invention cost)}))
;;      (take 10 with-jumps-sorted)))
;; (first with-jumps-sorted)
;; (db/get-system-by-id (:id (first (sort-by #(count (:jumps %1)) with-jumps))))

;; (filter #(= (get %1 :id) 30000184)
;;         with-jumps-sorted)

;; (count with-jumps)

;; (:jumps (second with-jumps))

;; (count 
;;  (get-shortest-path (:solarSystemID (db/get-system-by-name "Bamiette"))
;;                     (:solarSystemID (db/get-system-by-name "Jita"))
;;                     (fn [id]
;;                       true)))
;;                       (println (:security (db/get-system-by-id id)))
                      

;; (eve.navigation
;; ((:id (first systems))

;; (count foo)

;; (println foo)


