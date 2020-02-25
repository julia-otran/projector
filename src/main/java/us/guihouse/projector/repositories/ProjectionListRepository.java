package us.guihouse.projector.repositories;

import us.guihouse.projector.enums.ProjectionListItemType;
import us.guihouse.projector.models.*;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectionListRepository {
    public List<SimpleProjectionList> activeLists() throws SQLException {
        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement("SELECT id, title FROM projection_lists WHERE active = 1 ORDER BY id DESC");

        try {
            ResultSet rs = stmt.executeQuery();
            List<SimpleProjectionList> list = new ArrayList<>();

            while (rs.next()) {
                SimpleProjectionList projectionList = new SimpleProjectionList();

                projectionList.setTitle(rs.getString("title"));
                projectionList.setId(rs.getInt("id"));

                list.add(projectionList);
            }

            return list;
        } finally {
            stmt.close();
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

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
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
        } finally {
            stmt.close();
        }

        return items;
    }

    public Map<String, String> getItemProperties(long itemId) throws SQLException {
        Map<String, String> properties = new HashMap<>();

        String sql = "SELECT key, value "
                + "FROM projection_list_item_properties "
                + "WHERE projection_list_item_id = ?";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setLong(1, itemId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                properties.put(rs.getString("key"), rs.getString("value"));
            }
        } finally {
            stmt.close();
        }

        return properties;
    }

    public ProjectionListItem createItem(SimpleProjectionList list, String title, ProjectionListItemType type) throws SQLException {
        ProjectionListItem item = new ProjectionListItem();
        item.setTitle(title);
        item.setType(type);

        String maxOrderSql = "SELECT MAX(order_number) FROM projection_list_items WHERE projection_list_id = ?";
        PreparedStatement orderStmt =  SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(maxOrderSql);

        try {
            orderStmt.setInt(1, list.getId());
            ResultSet rs = orderStmt.executeQuery();
            item.setOrder(rs.getInt(1) + 1);
        } finally {
            orderStmt.close();
        }

        String sql = "INSERT INTO projection_list_items(title, type, projection_list_id, order_number) VALUES(?, ?, ?, ?)";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setString(1, item.getTitle());
            stmt.setString(2, item.getType().name());
            stmt.setInt(3, list.getId());
            stmt.setInt(4, item.getOrder());
            stmt.execute();
        } finally {
            stmt.close();
        }

        ResultSet keys = stmt.getGeneratedKeys();
        keys.next();
        item.setId(keys.getInt(1));

        return item;
    }

    public void updateItemProperties(long itemId, Map<String, String> properties) throws SQLException {
        String sql = "DELETE FROM projection_list_item_properties WHERE projection_list_item_id = ?";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setLong(1, itemId);
            stmt.execute();
        } finally {
            stmt.close();
        }

        final String insertSql = "INSERT INTO projection_list_item_properties(projection_list_item_id, key, value) VALUES (?, ?, ?)";

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            PreparedStatement insert = SQLiteJDBCDriverConnection
                    .getConn()
                    .prepareStatement(insertSql);

            try {
                insert.setLong(1, itemId);
                insert.setString(2, entry.getKey());
                insert.setString(3, entry.getValue());
                insert.execute();
            } finally {
                insert.close();
            }
        }
    }

    public void updateItemTitle(ProjectionListItem projectionListItem, String newTitle) throws SQLException {
        projectionListItem.setTitle(newTitle);

        String sql = "UPDATE projection_list_items SET title = ? WHERE id = ?";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setString(1, projectionListItem.getTitle());
            stmt.setLong(2, projectionListItem.getId());
            stmt.execute();
        } finally {
            stmt.close();
        }
    }
}
