(ns query.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.core :as h]
            [query.core :as q]
            [clj-json.core :as json]
  )
  (:use [hiccup.page :only [html5 include-css include-js]])
)


(defn home [& {:keys [explanation]}]
  (html5
    [:head
      [:title "Campari"]
      (include-css 
        "/css/bootstrap.min.css"
        "/css/bootstrap-theme.min.css"
        "/css/jumbotron.css"
        "/css/compari.css"
      )
     (include-js
       "http://code.jquery.com/jquery-1.10.1.min.js"
       "/js/spin.min.js"
       "/js/underscore-min.js"
       "/js/compari.js"
      )
    ]

   [:body
    [:div {:class "container"}
      [:div {:class "jumbotron"}
        [:h1 "compari"]
        [:h2 {:class "subtitle"} "comparison engine"]
        [:form {:id "query-form" :method "POST"}
          [:input {:type "text" :id "query" :name "query" :class "query"}]
          [:input {:type "submit" :id "query-submit" :class "query-submit" :value "Go" :disabled "disabled"}]
          [:p {:id "parse"} "&nbsp;"]
          [:p {:class "examples"} 
           "What scientist is similar to Albert Einstein?"
           [:br]
           "Compare Bill Gates to Steve Jobs."
           [:br]
           "What is the best film related to Anime?"
           [:br]
           "Who is the best philosopher?"
          ]

        ]
      ]
      [:div {:class "row marketing"}
       [:div {:id "result"}]
      ]
      [:div {:class "footer"}
        [:p "&copy; Thomas Dimson 2013"]
      ]
    ]
   ]
))

(defn json-response [data & [status]]
    {:status (or status 200)
        :headers {"Content-Type" "application/json"}
        :body (json/generate-string data)})

(defn- query-request [query]
  (let [template (q/extract-query-template query)]
    (if (= :failure (:type template))
      (json-response {:error "Could not parse query"} 400)
      (json-response (q/perform-query template))
    )
))

(defn query-parse [query] (json-response 
  {:parse (q/extract-query-template query)}
))

(defroutes app-routes
  (GET "/" [] (home))
  (GET "/parse" [query] (query-parse query))
  (GET "/query" [query] (query-request query))
  (route/resources "/")
  (route/not-found "404 Not found")
)


(def app
  (handler/site app-routes))
