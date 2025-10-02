(ns cronut.job-test
  (:require [clojure.test :refer [deftest is]]
            [cronut.job :as job])
  (:import (cronut.job ProxyJob SerialProxyJob)
           (org.quartz Job JobKey)))

(def job-keys [:fullName
               :jobClass
               :description
               :durable
               ;; note: this spelling mistake remains in 2.4.0, fixed in 2.5.0
               ;; https://github.com/quartz-scheduler/quartz/pull/1165
               :concurrentExectionDisallowed
               :jobDataMap])

(def reify-job (reify Job
                 (execute [_ _])))

(deftest job-detail-meta

  (is (= {:fullName                     "job-group.job-name"
          :jobClass                     SerialProxyJob
          :description                  "desc3"
          :durable                      true
          :concurrentExectionDisallowed true
          :jobDataMap                   {"job-impl" reify-job}}
         (-> (job/detail reify-job {:name                           "job-name"
                                    :group                          "job-group"
                                    :description                    "desc3"
                                    :durable?                       true
                                    :recover?                       true
                                    :disallow-concurrent-execution? true})
             (bean)
             (select-keys job-keys))))

  ;; :name is required before :name :group identity takes effect
  (is (= {:group                        JobKey/DEFAULT_GROUP
          :jobClass                     ProxyJob
          :description                  "desc3"
          :durable                      false
          :concurrentExectionDisallowed false
          :jobDataMap                   {"job-impl" reify-job}}
         (-> (job/detail reify-job {:group                          "job-group"
                                    :description                    "desc3"
                                    :durable?                       false
                                    :recover?                       false
                                    :disallow-concurrent-execution? false})
             (bean)
             (select-keys (conj (rest job-keys) :group))))))

(deftest job-detail-concurrency

  ;; global concurrentExecutionDisallowed? = false
  (is (= {:jobClass                     ProxyJob
          :description                  nil
          :durable                      false
          :concurrentExectionDisallowed false
          :jobDataMap                   {"job-impl" reify-job}}
         (-> (job/detail reify-job false)
             (bean)
             (select-keys job-keys)
             (dissoc :fullName))))

  ;; global concurrentExecutionDisallowed? = true
  (is (= {:jobClass                     SerialProxyJob
          :description                  nil
          :durable                      false
          :concurrentExectionDisallowed true
          :jobDataMap                   {"job-impl" reify-job}}
         (-> (job/detail reify-job {:disallow-concurrent-execution? true})
             (bean)
             (select-keys job-keys)
             (dissoc :fullName)))))
