package com.querylens.persistence.recommendation;

import com.querylens.persistence.core.AbstractWorkspaceRepository;
import com.querylens.recommendation.model.Recommendation;
import com.querylens.recommendation.model.RecommendationStatus;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class RecommendationRepository extends AbstractWorkspaceRepository {

    public RecommendationRepository(Path workspaceDatabase) {
        super(workspaceDatabase);
    }

    public List<Recommendation> saveAll(long analysisId, List<String> messages) {
        List<Recommendation> recommendations = new ArrayList<>();
        for (String message : messages) {
            recommendations.add(save(analysisId, message));
        }
        return List.copyOf(recommendations);
    }

    public List<Recommendation> findAll() {
        String query = "SELECT id, analysis_id, message, status FROM recommendations ORDER BY id DESC";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(query);
             ResultSet rows = statement.executeQuery()) {
            List<Recommendation> recommendations = new ArrayList<>();
            while (rows.next()) recommendations.add(read(rows));
            return List.copyOf(recommendations);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load recommendations.", exception);
        }
    }

    public Recommendation findById(long id) {
        String query = "SELECT id, analysis_id, message, status FROM recommendations WHERE id = ?";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(query)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) return read(rows);
            }
            throw new IllegalArgumentException("Recommendation not found.");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load recommendation.", exception);
        }
    }

    public void updateStatus(long id, RecommendationStatus status) {
        String update = "UPDATE recommendations SET status = ? WHERE id = ?";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(update)) {
            statement.setString(1, status.name());
            statement.setLong(2, id);
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Recommendation not found.");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not update recommendation.", exception);
        }
    }

    public void delete(long id) {
        try (var connection = openConnection();
             var statement = connection.prepareStatement("DELETE FROM recommendations WHERE id = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Recommendation not found.");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not delete recommendation.", exception);
        }
    }

    private Recommendation save(long analysisId, String message) {
        String insert = "INSERT INTO recommendations(analysis_id, message) VALUES (?, ?)";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, analysisId);
            statement.setString(2, message);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return new Recommendation(keys.getLong(1), analysisId, message, RecommendationStatus.PENDING);
            }
            throw new IllegalStateException("Could not retrieve recommendation ID.");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save recommendation.", exception);
        }
    }

    private Recommendation read(ResultSet row) throws Exception {
        return new Recommendation(row.getLong("id"), row.getLong("analysis_id"), row.getString("message"),
                RecommendationStatus.valueOf(row.getString("status")));
    }
}

