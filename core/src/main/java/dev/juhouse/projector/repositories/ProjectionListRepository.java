package dev.juhouse.projector.repositories;

import dev.juhouse.projector.enums.ProjectionListItemType;
import dev.juhouse.projector.models.ProjectionListItem;
import dev.juhouse.projector.models.SimpleProjectionList;
import dev.juhouse.projector.other.SQLiteJDBCDriverConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectionListRepository {
    public List<SimpleProjectionList> activeLists() throws SQLException {

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement("SELECT id, title FROM projection_lists WHERE active = 1 ORDER BY id DESC")) {
            ResultSet rs = stmt.executeQuery();
            List<SimpleProjectionList> list = new ArrayList<>();

            while (rs.next()) {
                SimpleProjectionList projectionList = new SimpleProjectionList();

                projectionList.setTitle(rs.getString("title"));
                projectionList.setId(rs.getInt("id"));

                list.add(projectionList);
            }

            return list;
        }
    }

    public SimpleProjectionList createList(String title) throws SQLException {
        SimpleProjectionList projectionList = new SimpleProjectionList();
        projectionList.setTitle(title);

        String sql = "INSERT INTO projection_lists(title) VALUES(?)";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        stmt.setString(1, projectionList.getTitle());
        stmt.execute();

        ResultSet keys = stmt.getGeneratedKeys();
        keys.next();
        projectionList.setId(keys.getInt(1));

        return projectionList;
    }

    public List<ProjectionListItem> getItems(int projectionListId) throws SQLException {
        List<ProjectionListItem> items = new ArrayList<>();
        String sql = "SELECT id, title, type, order_number "
                + "FROM projection_list_items "
                + "WHERE projection_list_id = ? "
                + "ORDER BY order_number ASC";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setInt(1, projectionListId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProjectionListItem item = new ProjectionListItem();
                item.setId(rs.getLong("id"));
                item.setTitle(rs.getString("title"));
                item.setType(ProjectionListItemType.valueOf(rs.getString("type")));
                item.setOrder(rs.getInt("order_number"));
                items.add(item);
            }
        }

        return items;
    }

    public Map<String, String> getItemProperties(long itemId) throws SQLException {
        Map<String, String> properties = new HashMap<>();

        String sql = "SELECT key, value "
                + "FROM projection_list_item_properties "
                + "WHERE projection_list_item_id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                properties.put(rs.getString("key"), rs.getString("value"));
            }
        }

        return properties;
    }

    public ProjectionListItem createItem(SimpleProjectionList list, String title, ProjectionListItemType type) throws SQLException {
        ProjectionListItem item = new ProjectionListItem();
        item.setTitle(title);
        item.setType(type);

        String maxOrderSql = "SELECT MAX(order_number) FROM projection_list_items WHERE projection_list_id = ?";

        try (PreparedStatement orderStmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(maxOrderSql)) {
            orderStmt.setInt(1, list.getId());
            ResultSet rs = orderStmt.executeQuery();
            item.setOrder(rs.getInt(1) + 1);
        }

        String sql = "INSERT INTO projection_list_items(title, type, projection_list_id, order_number) VALUES(?, ?, ?, ?)";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try (stmt) {
            stmt.setString(1, item.getTitle());
            stmt.setString(2, item.getType().name());
            stmt.setInt(3, list.getId());
            stmt.setInt(4, item.getOrder());
            stmt.execute();
        }

        ResultSet keys = stmt.getGeneratedKeys();
        keys.next();
        item.setId(keys.getInt(1));

        return item;
    }

    public void updateItemProperties(long itemId, Map<String, String> properties) throws SQLException {
        SQLiteJDBCDriverConnection
                .getConn()
                .setAutoCommit(false);

        String sql = "DELETE FROM projection_list_item_properties WHERE projection_list_item_id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setLong(1, itemId);
            stmt.execute();
        }

        final String insertSql = "INSERT INTO projection_list_item_properties(projection_list_item_id, key, value) VALUES (?, ?, ?)";

        try (PreparedStatement insert = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(insertSql)) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                insert.setLong(1, itemId);
                insert.setString(2, entry.getKey());
                insert.setString(3, entry.getValue());
                insert.execute();
            }
        }

        SQLiteJDBCDriverConnection
                .getConn()
                .commit();
    }

    public void updateItemTitle(ProjectionListItem projectionListItem, String newTitle) throws SQLException {
        projectionListItem.setTitle(newTitle);

        String sql = "UPDATE projection_list_items SET title = ? WHERE id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setString(1, projectionListItem.getTitle());
            stmt.setLong(2, projectionListItem.getId());
            stmt.execute();
        }
    }

    public void updateItemSort(ProjectionListItem item) throws SQLException {
        String sql = "UPDATE projection_list_items SET order_number = ? WHERE id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setInt(1, item.getOrder());
            stmt.setLong(2, item.getId());
            stmt.execute();
        }
    }

    public void deleteItem(ProjectionListItem item) throws SQLException {
        updateItemProperties(item.getId(), new HashMap<>());

        String sql = "DELETE FROM projection_list_items WHERE id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setLong(1, item.getId());
            stmt.execute();
        }
    }

    public void deleteList(SimpleProjectionList selectedItem) throws SQLException {
        String sql = "UPDATE projection_lists SET active = 0 WHERE id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setInt(1, selectedItem.getId());
            stmt.execute();
        }
    }
}
