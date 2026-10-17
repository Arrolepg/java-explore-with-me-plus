package ru.practicum.ewm.service.compilation.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import ru.practicum.ewm.service.compilation.model.Compilation;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;

import java.util.List;

public class CompilationSearchRepositoryImpl implements CompilationSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Compilation> findCompilations(Boolean pinned, CompilationPaginationParams params) {
        String jpql = "select c from Compilation c";
        if (pinned != null) {
            jpql += " where c.pinned = :pinned";
        }
        TypedQuery<Compilation> query = entityManager.createQuery(jpql + " order by c.id", Compilation.class);
        if (pinned != null) {
            query.setParameter("pinned", pinned);
        }
        return query.setFirstResult(params.getFrom()).setMaxResults(params.getSize()).getResultList();
    }
}
