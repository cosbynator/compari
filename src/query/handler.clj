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
       "/js/compari.js"
      )
    ]

   [:body
    [:div {:class "container"}
      [:div {:class "jumbotron"}
        [:h1 "Compari"]
        [:form {:method "POST"}
          [:input {:type "text" :id "query" :name "query" :class "query"}]
          [:input {:type "submit" :id "query-submit" :class "query-submit" :value "Go" :disabled "disabled"}]
          [:p {:id "parse"} "&nbsp;"]
          [:p {:class "examples"} 
           "e.g. Who is similar to Albert Einstein?"
          ]

        ]
      ]
      [:div {:class "row marketing"}]
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
  (let [query-template (q/extract-query-template query)]
     (if (not query-template)
         (home :explanation "Unable to parse query")
         (let [template-explanation (q/template-explanation query-template)]
          (home :explanation template-explanation)
         )
     )
  )
)

(defn query-parse [query] (json-response 
  {:parse (q/extract-query-template query)}
))

(defroutes app-routes
  (GET "/" [] (home))
  (POST "/" [query] (query-request query))
  (GET "/parse" [query] (query-parse query))
  (route/resources "/")
  (route/not-found "404 Not found")
)


(def app
  (handler/site app-routes))
