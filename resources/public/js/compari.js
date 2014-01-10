var delay = (function(){
  var timer = 0;
  return function(callback, ms){
    clearTimeout (timer);
    timer = setTimeout(callback, ms);
  };
})();

$(document).ready(function() {
    "use strict";

    var spinner = new Spinner();
    
    function parseExplanation(parse) {
        var start;
        switch(parse.type) {
            case "knn":
                start = "K-Nearest Neighbors of <em>" + parse["article-title"] + "</em>";
                if(parse.infobox) {
                    start += " with infobox <em>" + parse.infobox + "</em>";
                }

                start += "<small> using the " + parse.norm + " norm and " + parse.features + "</small>";
                return start;
            case "compare":
                start = "Comparing <em>" + parse["first-article-title"] + "</em>" + 
                                " with <em> "+ parse["second-article-title"] + "</em>";
                if(parse.topics.length > 0) {
                    start += " in the topic matching <em>" + parse.topics.join(" or ") + "</em>";
                }
                return start;
            case "top_k":
                start = "Top K articles";
                if(parse.infobox !== null) {
                    start += " with infobox <em> " + parse.infobox + " </em>";
                }
                if(parse.topics.length > 0) {
                    start += " in the topic matching <em>" + parse.topics.join(" or ") + "</em>";
                }

                   start += "<small> using " + parse.features + "</small>";
                return start;
            default:
                return "No explanation";
        }
    }

    var helpers = {
        wikilink: function(title) {
            return "<a href=\"http://en.wikipedia.org/wiki/" + title.replace(" ", "_") + "\">" + title + "</a>";
        }
    };

    var comparisonTemplate = _.template(
        "<div class='compare-result text-center'>" +
            "<p>We estimate that <strong> <%= wikilink(t1) %> </strong> is</p>" +
            "<div class='compare-ratio'><%=ratio%></div>" +
            "<p>more influential than <strong> <%= wikilink(t2) %> </strong></p>" + 
        "</div>" +
        "<p class='small'><ul>" +
            "<li>LDA-Cosine: <%= (lda_cosine * 100).toFixed(1) %>%" +
            "<li>LDA-L2-Sim: <%= lda_l2_sim.toFixed(3) %>" +
            "<li>TSPR-Cosine: <%= (tspr_cosine * 100).toFixed(1) %>%" +
            "<li>TSPR-L2-Sim: <%= tspr_l2_sim.toFixed(3) %>" +
            "<li>LSPR-Cosine: <%= (lspr_cosine * 100).toFixed(1) %>%" +
            "<li>LSPR-L2-Sim: <%= lspr_l2_sim.toFixed(3) %>" +
        "</ul></p>"
    );

    function renderComparison(o) {
        var ratio = o.ratio.toFixed(1) + "x";
        var t1 = o["first-article"].title;
        var t2 = o["second-article"].title;
        console.log(o);
        return comparisonTemplate($.extend(helpers, {
            t1: t1,
            t2: t2,
            ratio: ratio,
            lda_cosine: o["lda-cosine-similarity"],
            lda_l2_sim: o["lda-l2-similarity"],
            tspr_cosine: o["tspr-cosine-similarity"],
            tspr_l2_sim: o["tspr-l2-similarity"],
            lspr_cosine: o["lspr-cosine-similarity"],
            lspr_l2_sim: o["lspr-l2-similarity"]
        }));
    }
    
    var knnTemplate = _.template(
        "<h2> The Nearest Neighbors of <%= source.title %> </h2>" +
        "<table class='table table-striped'>" +
            "<thead>" +
                "<tr><th> # </th> <th> Neighbor </th></tr>" +
            "</thead>" +
            "<tbody>" +
            "<% _.each(neighbors, function(article) { %>" +
                "<tr>" +
                    "<td> <%= article.position %> </td> <td> <%= wikilink(article.title)%> </td>" +
                "</tr>" +
            "<% }); %>" +
            "</tbody>" +
        "</table>"
    );

    function renderKnn(o) {
        if(o.neighbors.length === 0) {
            return "No results found";
        }

        var neighbor;
        var source = o["source-article"];
        for(var i = 0; i < o.neighbors.length; i++) {
            neighbor = o.neighbors[i];
            neighbor.position = i+1;
        }

        return knnTemplate($.extend(helpers, {
            source: source,
            neighbors: o.neighbors,
        }));
    }


    var topKTemplate = _.template(
        "<h2> The Top <%= infobox %> </h2>" +
        "<% topicFragment %>" +
        "<table class='table table-striped'>" +
            "<thead>" +
                "<tr><th> # </th> <th> <%=infobox%> </th> <th> Relative </th> </tr>" +
            "</thead>" +
            "<tbody>" +
            "<% _.each(articles, function(article) { %>" +
                "<tr>" +
                    "<td> <%= article.position %> </td> <td> <%= wikilink(article.title)%> </td>" +
                    "<td> <%= (article.relative * 100).toFixed(0) %>% </td>" +
                "</tr>" +
            "<% }); %>" +
            "</tbody>" +
        "</table>"
    );

    function renderTopK(o) {
        if(o.articles.length === 0) {
            return "No results found";
        }

        console.dir(o);

        var topicFragment = "";
        if(o["topic-words"].length > 0) {
            topicFragment = "<p><strong>Topic</strong>: " + o["topic-words"].join(",") + "</p>";
        }

        var features = o.features;
        var topic = o["topic-index"];
        var firstArticle = o.articles[0];
        var article;
        for(var i = 0; i < o.articles.length; i++) {
            article = o.articles[i];
            article.position = i+1;
            if (features === 'x2') {
                article.relative = (article.tspr[topic] - article.tspr[article.tspr.length -1]) / Math.sqrt(article.tspr[article.tspr.length -1]);
            } else if (features === 'emass') {
                article.relative = (article.lda[topic] * article.tspr[article.tspr.length -1]) / 
                    (firstArticle.lda[topic] * firstArticle.tspr[firstArticle.tspr.length -1]);
            } else {
                article.relative = article[features][topic] / firstArticle[features][topic];
            }

            console.log(article.title + ": " + (article.lda[topic] * article.tspr[article.tspr.length -1]));
        }

        return topKTemplate($.extend(helpers, {
            topicFragment: topicFragment,
            articles: o.articles,
            infobox: (o.infobox ? o.infobox : "Thing")
        }));
    }

    $("#query").keyup(function() {
        var self = this;
        delay(function() {
            $.get("/parse", {query: $(self).val()}, function(data) { 
                if(data.parse.type === "failure") {
                    $("#query-submit").prop('disabled', true);
                    var col = data.parse.explanation.column;
                    var start = $(self).val().substring(0, col);
                    var end = $(self).val().substring(col);
                    $("#parse").html("Error at col " + col +": &quot;<em>" + start + "</em><strong>&loz;&loz;</strong>" + end + "&quot;");
                } else {
                    $("#query-submit").prop('disabled', false);
                    $("#parse").html("Interpretation: " + parseExplanation(data.parse));
                }
            });
        }, 50); 
        return true;
    });

    $("#query-form").submit(function() {
        $("#result").html("");
        spinner.spin(document.getElementById("result"));
        $.get("/query", {query: $("#query").val()})
            .done(function(data) {
                var renderFn; 
                if(data && data.type) {
                    switch(data.type) {
                        case "compare": 
                            renderFn = renderComparison;
                            break;
                        case "top_k":
                            renderFn = renderTopK;
                            break;
                        case "knn":
                            renderFn = renderKnn;
                            break;
                    }

                    spinner.stop();
                    if(renderFn) {
                        $("#result").html(renderFn(data));
                    } else {
                        $("#result").html("Error finding render function :(");
                    }
                }
            })
            .fail(function() {
                spinner.stop();
                $("#result").html("Error response :(");
            });
        return false;
    });
});
