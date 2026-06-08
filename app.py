import pandas as pd
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.metrics.pairwise import cosine_similarity

movies = pd.read_csv("movies.csv")

movies["genres"] = movies["genres"].fillna("")

vectorizer = CountVectorizer(tokenizer=lambda x: x.split("|"))
genre_matrix = vectorizer.fit_transform(movies["genres"])
similarity = cosine_similarity(genre_matrix)

def recommend(movie_title, top_n=5):
    if movie_title not in movies["title"].values:
        return ["Movie not found"]
    idx = movies[movies["title"] == movie_title].index[0]
    scores = list(enumerate(similarity[idx]))
    scores = sorted(scores, key=lambda x: x[1], reverse=True)[1:top_n+1]
    return movies.iloc[[i[0] for i in scores]]["title"].tolist()

if __name__ == "__main__":
    movie = input("Enter movie title: ")
    print("\nRecommended Movies:")
    for m in recommend(movie):
        print("-", m)
