package fmi.uni.masters;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BookInfo {
    private String name;
    private List<String> authors = new ArrayList<>();
    private List<String> genres = new ArrayList<>();
    private List<String> ratings = new ArrayList<>();
    private List<String> inferredTypes = new ArrayList<>();
    private int publicationYear;
    private List<String> sameAuthorBooks = new ArrayList<>();
    private List<String> similarBooks = new ArrayList<>();
    private List<String> publishers = new ArrayList<>();
    private String seriesName;
    private int seriesOrder;

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(name.replaceAll("_", " ")).append("\n");

        if(!authors.isEmpty()) {
            sb.append("Author: ").append(String.join(", ", authors).replaceAll("_", " ")).append("\n");
        }

        if (!genres.isEmpty()) {
            sb.append("Genres: ").append(String.join(", ", genres)).append("\n");
        }

        if (publicationYear > 0) {
            sb.append("Published Year: ").append(publicationYear).append("\n");
        }

        if(!ratings.isEmpty()) {
            sb.append("Rating: ").append(String.join(", ", ratings)).append("\n");
        }

        if(!publishers.isEmpty()) {
            sb.append("Publisher: ").append(String.join(", ", publishers).replaceAll("_", " ")).append("\n");
        }

        if(!inferredTypes.isEmpty()) {
            List<String> filteredTypes = new ArrayList<>();
            for (String type : inferredTypes) {
                if (!type.startsWith("LikedBy_") && !type.startsWith("RecommendedTo_")) {
                    filteredTypes.add(type);
                }
            }

            if (!filteredTypes.isEmpty()) {
                sb.append("Inferred: ").append(String.join(", ", filteredTypes)).append("\n");
            }
        }

        if (seriesName != null && !seriesName.isEmpty()) {
            sb.append("Belongs to Series: ").append(seriesName.replaceAll("_", " ")).append("\n");

            if (seriesOrder > 0) {
                sb.append("Series Order: ").append(seriesOrder).append("\n");

                if (seriesOrder == 1 || inferredTypes.contains("SeriesFirstVolume")) {
                    sb.append("Series Status: First Volume (Book 1 in the series)\n");
                } else {
                    sb.append("Series Status: Sequel / Continuation\n");
                }
            } else {
                sb.append("Series Status: Number not specified yet\n");
            }
        }

        if (!sameAuthorBooks.isEmpty()) {
            sb.append("Books from the same author: ").append(String.join(", ", sameAuthorBooks).replaceAll("_", " ")).append("\n");
        }
        if (!similarBooks.isEmpty()) {
            sb.append("Similar books: ").append(String.join(", ", similarBooks).replaceAll("_", " "));
        }

        return sb.toString();
    }
}
