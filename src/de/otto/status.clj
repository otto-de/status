(ns de.otto.status)

(defonce scores {:ok      0
                :warning 1
                :error   2})

(defn aggregate-forgiving [msgs status-map]
  (let [best (apply min-key scores (map :status (vals status-map)))
        score (if (= :ok best) :ok :error)]
    (assoc {:status score :message (score msgs)}
           :statusDetails status-map)))

(defn aggregate-strictly [msgs status-map]
  (let [worst (apply max-key scores (map :status (vals status-map)))]
    (assoc {:status worst :message (worst msgs)}
           :statusDetails status-map)))

(defn status-detail
  ([id status message]
    (status-detail id status message {}))
  ([id status message extras]
    {id (assoc extras :status status :message message)}))

(defn forgiving-strategy [status-map]
  (aggregate-forgiving {:ok "at least one ok" :error "none ok"} status-map))

(defn strict-strategy [status-map]
  (aggregate-strictly {:ok "all ok" :warning "some warnings" :error "none ok"} status-map))

(defn aggregate-status
  ([id strategy funs] (aggregate-status id strategy funs {}))
  ([id strategy funs extras]
    {id
     (into extras
           (if (empty? funs)
             {:status :ok :message "no substatus"}
             (strategy (into {} (map #(%) funs)))))}))
