const fs = require('fs');
const path = require('path');

function upgradeClickables(dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            upgradeClickables(fullPath);
        } else if (fullPath.endsWith('.kt')) {
            let content = fs.readFileSync(fullPath, 'utf8');
            let updated = content.replace(/\.clickable\s*\{/g, '.minimumInteractiveComponentSize().clickable {');
            
            if (content !== updated) {
                // Ensure import for minimumInteractiveComponentSize exists or just use defaultMinSize(minHeight = 44.dp, minWidth = 44.dp)
                updated = content.replace(/\.clickable\s*\{/g, '.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp).clickable {');
                
                // Add import if missing
                if (!updated.includes('import androidx.compose.foundation.layout.defaultMinSize') && updated.includes('defaultMinSize')) {
                    updated = updated.replace(/import androidx\.compose\.foundation\.layout\.fillMaxWidth/, 
                    'import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.defaultMinSize');
                }
                
                fs.writeFileSync(fullPath, updated, 'utf8');
                console.log(`Updated clickables in ${fullPath}`);
            }
        }
    }
}

upgradeClickables('app/src/main/java/com/example');
