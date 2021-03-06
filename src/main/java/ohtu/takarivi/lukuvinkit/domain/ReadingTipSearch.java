package ohtu.takarivi.lukuvinkit.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import ohtu.takarivi.lukuvinkit.repository.ReadingTipRepository;

public class ReadingTipSearch {

    /**
     * Searches for reading tips with a single keyword.
     *
     * @param readingTipRepository The repository to search from.
     * @param username The user name of the user doing the search.
     * @param id The ID of the user doing the search.
     * @param keyword The keyword to search with.
     * @return The reading tips found with this search.
     */
    public static List<ReadingTip> searchSimple(ReadingTipRepository readingTipRepository, String username, Long id, String keyword) {
        Specification<ReadingTip> specRt = getSpecSearchSimple(id, username, keyword.toLowerCase());
        return readingTipRepository.findAll(specRt);
    }

    private static Specification<ReadingTip> getSpecSearchSimple(Long id, String username, String keyword) {
        return new Specification<ReadingTip>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<ReadingTip> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                List<Predicate> predicates = new ArrayList<>();
                // user can only search for their own tips
                predicates.add(builder.like(root.<String>get("customUser").get("username"), username));
                
                Predicate allowed = builder.disjunction();
                // allow the keyword to appear in the title, description, author, ISBN or URL
                allowed = builder.or(allowed, builder.like(builder.lower(root.get("title")), "%" + keyword + "%"));
                allowed = builder.or(allowed, builder.like(builder.lower(root.get("description")), "%" + keyword + "%"));
                allowed = builder.or(allowed, builder.like(builder.lower(root.get("author")), "%" + keyword + "%"));
                // ignore dashes/hyphens in ISBN search
                allowed = builder.or(allowed, builder.like(builder.function("REPLACE", 
                                                            String.class, 
                                                            root.get("isbn"), 
                                                            builder.literal("-"), 
                                                            builder.literal("")), 
                                                            "%" + keyword.replace("-", "") + "%"));
                allowed = builder.or(allowed, builder.like(root.get("url"), "%" + keyword + "%"));
                
                // tags
                Path<Set<String>> tagsPath = root.join("tags", JoinType.LEFT);
                Predicate tagsMatch = builder.disjunction();
                for (String word: keyword.split(" ")) {
                    if (!word.trim().isEmpty()) {
                        tagsMatch = builder.or(tagsMatch, builder.like(builder.lower(tagsPath.get("name")), "%" + word.trim() + "%"));
                    }
                }
                allowed = builder.or(allowed, tagsMatch);

                predicates.add(allowed);

                return builder.and(predicates.toArray(new Predicate[]{}));
            }
        };
    }

    /**
     * Searches for reading tips with an advanced search.
     *
     * @param readingTipRepository The repository to search from.
     * @param customUser The CustomUser instance of the user doing the search.
     * @param id The ID of the user doing the search.
     * @param title The title to search for, or empty if title isn't searched
     * for.
     * @param description The description to search for, or empty if description
     * isn't searched for.
     * @param url The URL to search for, or empty if URL isn't searched for.
     * @param author The author to search for, or empty if author isn't searched
     * for.
     * @param tags The tags to search for, separated with spaces.
     * @param category List of allowed categories for the search results.
     * @param unreadstatus List of allowed unread/read statuses for the search
     * results.
     * @return The reading tips found with this search.
     */
    public static List<ReadingTip> searchAdvanced(ReadingTipRepository readingTipRepository, CustomUser customUser, Long id, String title,
            String description, String url, String author, String tags, List<String> category, List<String> unreadstatus) {
        return readingTipRepository.findAll(getSpec(customUser, id, title, description, url, author, tags, category, unreadstatus));
    }

    private static Specification<ReadingTip> getSpec(CustomUser customUser, Long id, String title, String description, String url, String author, String tags, List<String> category, List<String> unreadstatus) {
        return new Specification<ReadingTip>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<ReadingTip> root, CriteriaQuery<?> query,
                    CriteriaBuilder builder) {
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(builder.like(root.<String>get("customUser").get("username"), customUser.getUsername()));

                if (!title.isEmpty()) { // title must match
                    predicates.add(builder.like(builder.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
                }
                if (!description.isEmpty()) { // description must match
                    predicates.add(builder.like(builder.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
                }
                if (!url.isEmpty()) { // URL must match
                    predicates.add(builder.like(builder.lower(root.get("url")), "%" + url.toLowerCase() + "%"));
                }
                if (!author.isEmpty()) { // author must match
                    predicates.add(builder.like(builder.lower(root.get("author")), "%" + author.toLowerCase() + "%"));
                }

                Predicate allowedTypes = builder.disjunction();
                // set allowed types based on checkbox values
                final String[] validCategories = new String[] {"books", "articles", "videos", "links"};
                for (String validCategory: validCategories) {
                    if (category.contains(validCategory)) {
                        allowedTypes = builder.or(allowedTypes, builder.equal(root.<ReadingTipCategory>get("category"), ReadingTipCategory.getByName(validCategory)));
                    }
                }
                predicates.add(allowedTypes);

                if (!unreadstatus.contains("unread")) { // must not be unread
                    predicates.add(builder.isTrue(root.get("isRead")));
                }
                if (!unreadstatus.contains("read")) { // must not be read
                    predicates.add(builder.isFalse(root.get("isRead")));
                }

                // tags
                if (!tags.trim().isEmpty()) {
                    Path<Set<String>> tagsPath = root.join("tags", JoinType.LEFT);
                    Predicate tagsMatch = builder.disjunction();
                    for (String word: tags.split(" ")) {
                        if (!word.trim().isEmpty()) {
                            tagsMatch = builder.or(tagsMatch, builder.like(builder.lower(tagsPath.get("name")), "%" + word.trim() + "%"));
                        }
                    }
                    predicates.add(tagsMatch);
                }

                return builder.and(predicates.toArray(new Predicate[]{}));
            }
        };
    }

    private ReadingTipSearch() {
    }
}
