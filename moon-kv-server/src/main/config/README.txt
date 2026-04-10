MOON-KV Server 1.0.0
====================

Thank you for using MOON-KV!

Getting Started
---------------
1. Configure the server by editing config/server.properties
2. Start the server:
   - Linux/Mac: ./bin/start.sh
   - Windows: bin\start.bat
3. Access the Dashboard: http://localhost:4070/
4. Stop the server:
   - Linux/Mac: ./bin/stop.sh
   - Windows: bin\stop.bat

Directory Structure
-------------------
bin/         - Startup and shutdown scripts
lib/         - Java libraries
config/      - Configuration files
logs/        - Log files
data/        - Data files (WAL)

API Endpoints
-------------
- GET    /api/v1/health         - Health check
- GET    /api/v1/stats          - System statistics
- GET    /api/v1/stats/memory   - Memory statistics
- GET    /api/v1/config         - Configuration
- GET    /api/v1/kv             - List all keys
- GET    /api/v1/kv/{key}       - Get a key-value
- PUT    /api/v1/kv/{key}       - Set a key-value
- DELETE /api/v1/kv/{key}       - Delete a key-value
- POST   /api/v1/kv/{key}/ttl   - Set TTL for a key

Documentation
-------------
For more information, visit: https://github.com/saki/moon-kv

Support
-------
If you encounter any issues, please report them at:
https://github.com/saki/moon-kv/issues
