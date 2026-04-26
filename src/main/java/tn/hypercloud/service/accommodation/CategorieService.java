package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import tn.hypercloud.entity.accommodation.Categorie;
import tn.hypercloud.payload.request.CategorieRequest;
import tn.hypercloud.payload.response.CategorieResponse;
import tn.hypercloud.repository.accommodation.CategorieRepository;
import tn.hypercloud.repository.user.UserRepository;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategorieService {

    private final CategorieRepository repo;
    private final UserRepository userRepo;

    // CREATE
    public CategorieResponse create(CategorieRequest req) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.HEBERGEUR) {
            if (repo.existsByNomCategorieAndOwnerId(req.getNomCategorie(), currentUser.getId())) {
                throw new RuntimeException("Catégorie déjà existante pour votre compte : " + req.getNomCategorie());
            }
        } else if (repo.existsByNomCategorieAndOwnerIsNull(req.getNomCategorie())) {
            throw new RuntimeException(
                    "Catégorie déjà existante : " + req.getNomCategorie());
        }

        Categorie categorie = Categorie.builder()
                .nomCategorie(req.getNomCategorie())
                .description(req.getDescription())
                .icone(req.getIcone())
                .statut(req.isStatut())
            .owner(currentUser.getRole() == Role.HEBERGEUR ? currentUser : null)
                .build();

        return toResponse(repo.save(categorie));
    }

    // GET ALL
    public List<CategorieResponse> getAll() {
        User currentUser = getCurrentUserOrNull();
        List<Categorie> categories = currentUser != null && currentUser.getRole() == Role.HEBERGEUR
            ? repo.findByOwnerId(currentUser.getId())
            : repo.findAll();

        return categories
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET ACTIVES
    public List<CategorieResponse> getActives() {
        User currentUser = getCurrentUserOrNull();
        List<Categorie> categories = currentUser != null && currentUser.getRole() == Role.HEBERGEUR
            ? repo.findByOwnerIdAndStatutTrue(currentUser.getId())
            : repo.findByStatutTrue();

        return categories
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET BY ID
    public CategorieResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    // UPDATE
    public CategorieResponse update(Integer id, CategorieRequest req) {
        User currentUser = getCurrentUser();

        Categorie categorie = findOrThrow(id);

        ensureCanManage(categorie, currentUser);

        categorie.setNomCategorie(req.getNomCategorie());
        categorie.setDescription(req.getDescription());
        categorie.setIcone(req.getIcone());
        categorie.setStatut(req.isStatut());

        return toResponse(repo.save(categorie));
    }

    // DELETE
    public void delete(Integer id) {
        User currentUser = getCurrentUser();
        Categorie categorie = findOrThrow(id);
        ensureCanManage(categorie, currentUser);
        repo.delete(categorie);
    }

    // FIND
    private Categorie findOrThrow(Integer id) {
        return repo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Catégorie introuvable : " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    private User getCurrentUserOrNull() {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email).orElse(null);
    }

    private void ensureCanManage(Categorie categorie, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) return;
        if (currentUser.getRole() == Role.HEBERGEUR && categorie.getOwner() != null && categorie.getOwner().getId().equals(currentUser.getId())) return;
        throw new RuntimeException("Accès refusé : vous ne pouvez gérer que vos propres catégories.");
    }

    // ENTITY → RESPONSE
    private CategorieResponse toResponse(Categorie categorie) {

        Long nbLogements = 0L;

        if (categorie.getLogements() != null) {
            nbLogements = (long) categorie.getLogements().size();
        }

        return CategorieResponse.builder()
                .idCategorie(categorie.getIdCategorie())
                .nomCategorie(categorie.getNomCategorie())
                .description(categorie.getDescription())
                .icone(categorie.getIcone())
                .statut(categorie.isStatut())
                .dateCreation(categorie.getDateCreation())
                .nbLogements(nbLogements)
                .build();
    }
    }
