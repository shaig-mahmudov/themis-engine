package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;

public class V6__MigrateModifierSources extends BaseJavaMigration {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        
        migrateTable(connection, "character_equipped_items", new String[]{"character_id", "item_id"});
        migrateTable(connection, "character_active_conditions", new String[]{"character_id", "condition_id"});
        migrateTable(connection, "character_equipped_weapons", new String[]{"character_id", "weapon_id"});
    }

    private void migrateTable(Connection connection, String tableName, String[] primaryKeys) throws Exception {
        String pkSelect = String.join(", ", primaryKeys);
        String selectSql = String.format("SELECT %s, modifiers_json FROM %s", pkSelect, tableName);
        
        String updateSql = String.format("UPDATE %s SET modifiers_json = ? WHERE %s = ? AND %s = ?", 
                tableName, primaryKeys[0], primaryKeys[1]);

        try (Statement selectStmt = connection.createStatement();
             ResultSet rs = selectStmt.executeQuery(selectSql);
             PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
            
            while (rs.next()) {
                String pk1Val = rs.getString(primaryKeys[0]);
                String pk2Val = rs.getString(primaryKeys[1]);
                String modifiersJson = rs.getString("modifiers_json");
                
                if (modifiersJson == null || modifiersJson.trim().isEmpty()) {
                    continue;
                }
                
                String migratedJson = migrateJson(modifiersJson);
                if (!migratedJson.equals(modifiersJson)) {
                    updateStmt.setString(1, migratedJson);
                    updateStmt.setString(2, pk1Val);
                    updateStmt.setString(3, pk2Val);
                    updateStmt.executeUpdate();
                }
            }
        }
    }

    private String migrateJson(String jsonStr) throws Exception {
        JsonNode root = objectMapper.readTree(jsonStr);
        if (!root.isObject()) {
            return jsonStr;
        }

        boolean modified = false;
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode modifiersArray = field.getValue();
            if (modifiersArray.isArray()) {
                for (JsonNode modifier : modifiersArray) {
                    if (modifier.isObject()) {
                        ObjectNode modObj = (ObjectNode) modifier;
                        JsonNode sourceNode = modObj.get("source");
                        if (sourceNode != null && sourceNode.isTextual()) {
                            String legacySourceStr = sourceNode.asText();
                            
                            ObjectNode newSourceNode = objectMapper.createObjectNode();
                            newSourceNode.put("id", legacySourceStr);
                            newSourceNode.put("name", legacySourceStr);
                            newSourceNode.put("type", "GENERIC");
                            
                            modObj.set("source", newSourceNode);
                            modified = true;
                        }
                    }
                }
            }
        }
        
        return modified ? objectMapper.writeValueAsString(root) : jsonStr;
    }
}
