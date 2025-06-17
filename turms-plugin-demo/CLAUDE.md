# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **turms-plugin-demo** - a demonstration project showing how to develop custom plugins for the Turms instant messaging system. It serves as a reference implementation for developers who want to extend Turms functionality through the plugin system.

### Architecture

**Plugin Type:** Turms Extension Plugin
- **Target Servers:** Both turms-gateway and turms-service
- **Extension Points:** ClientRequestTransformer
- **Purpose:** Demonstrates request interception and modification

**Core Components:**
- `MyPlugin.java`: Main plugin class extending TurmsPlugin
- `MyTurmsRequestHandler.java`: Request transformer implementation
- `plugin.yaml`: Plugin metadata and configuration
- `pom.xml`: Maven build configuration with plugin-specific setup

## Build and Development Commands

### Maven Commands

**Building:**
```bash
# Compile the plugin
mvn clean compile

# Package the plugin (creates shaded JAR)
mvn clean package

# The output will be: target/my-plugin-1.0.0.jar
```

**Code Quality:**
```bash
# Apply code formatting (Spotless)
mvn spotless:apply

# Check code formatting
mvn spotless:check
```

**Development Workflow:**
```bash
# 1. Make changes to plugin code
# 2. Format code
mvn spotless:apply

# 3. Build plugin
mvn clean package

# 4. Deploy to Turms server plugins directory
# cp target/my-plugin-1.0.0.jar /path/to/turms/plugins/
```

## Plugin Development Standards

**Code Quality Requirements:**
- **Java Version:** Java 21 (aligned with Turms server requirements)
- **Line length:** 100 characters maximum
- **Indentation:** 4 spaces (no tabs)
- **Import organization:** java|javax|jakarta, then im.turms, then static imports
- **License headers:** Apache License 2.0 (automatically applied by Spotless)
- **Formatting:** Eclipse formatter configuration required

**Plugin Architecture Patterns:**
- **Extension Points:** Use @ExtensionPointMethod annotation for hook methods
- **Reactive Programming:** Return Mono/Flux for asynchronous operations
- **Logging:** Use im.turms.server.common.infra.logging.core.logger.Logger
- **Validation:** Use Jakarta validation annotations (@NotNull, etc.)

## Development Environment Setup

**Requirements:**
- Java 21
- Maven 3.6+
- Access to Turms server (for testing)

**Missing Configuration Files:**
The following files are referenced in pom.xml but missing from the project:
- `codequality/eclipse.xml` - Eclipse formatter configuration
- `codequality/java-license-header.txt` - License header template

**Plugin Testing:**
1. Build the plugin: `mvn clean package`
2. Copy JAR to Turms plugins directory
3. Restart Turms server
4. Check logs for plugin loading confirmation
5. Test functionality through client requests

## Plugin Configuration

**plugin.yaml Structure:**
- `id`: Unique plugin identifier (com.mydomain.MyPlugin)
- `class`: Main plugin class name
- `compatible-servers`: Supported server types (turms-gateway, turms-service)
- `version`: Plugin version
- `provider`: Plugin provider/author
- `license`: Plugin license
- `description`: Plugin description

## Extension Points Available

**ClientRequestTransformer:**
- **Purpose:** Intercept and modify client requests before processing
- **Method:** `transform(ClientRequest clientRequest)`
- **Return:** `Mono<ClientRequest>`
- **Use Cases:** Request validation, content filtering, request modification

**Common Plugin Patterns:**
- Request interception and modification
- Response transformation
- Custom business logic injection
- Integration with external services
- Data validation and sanitization

## Testing Strategy

**Manual Testing:**
1. Use Turms client to send requests
2. Verify plugin behavior through server logs
3. Check request modifications take effect

**Integration Testing:**
- Test with actual Turms server instance
- Verify plugin compatibility with target server versions
- Test plugin loading/unloading lifecycle

## Common Development Tasks

**Adding New Extension Points:**
1. Implement required TurmsExtension interface
2. Add @ExtensionPointMethod annotation to hook methods
3. Register extension in MyPlugin.getExtensions()
4. Update plugin.yaml if needed

**Modifying Request Processing:**
1. Edit MyTurmsRequestHandler.transform() method
2. Handle different TurmsRequest.KindCase types
3. Apply necessary transformations
4. Return modified ClientRequest wrapped in Mono

**Plugin Deployment:**
1. Build: `mvn clean package`
2. Copy JAR to server plugins directory
3. Restart server or use hot-reload if supported
4. Monitor logs for successful loading

## Dependencies and Compatibility

**Core Dependencies:**
- `turms-service`: 0.10.0-SNAPSHOT (provided scope)
- Reactor Core: For reactive programming
- Jakarta Validation: For input validation

**Version Compatibility:**
- Plugin API version: 0.10.0-SNAPSHOT
- Ensure compatibility with target Turms server version
- Update dependency version when upgrading Turms

## Troubleshooting

**Common Issues:**
- Missing codequality configuration files
- Plugin not loading: Check plugin.yaml syntax and class path
- ClassNotFoundException: Verify all dependencies are properly shaded
- Extension point not working: Ensure @ExtensionPointMethod annotation is present

**Debug Tips:**
- Enable DEBUG logging for plugin-related packages
- Check plugin loading logs during server startup
- Verify JAR file contains all necessary classes
- Test plugin isolation by temporarily removing other plugins