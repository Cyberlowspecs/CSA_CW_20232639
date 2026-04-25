> [!CAUTION]
> **🚨 NOT FOR PRODUCTION USE 🚨**
> 
> This is **NOT** a demonstration of my professional work or any demonstration of my skills. Please **DO NOT** clone or use this project in production. The libraries used contain known security vulnerabilities, and many components are completely outdated. 
> 
> Furthermore, this repository will **<mark>vanish</mark>** in a few weeks.

# Smart Campus RESTful API Coursework Report
**Coursework for Client Server Architecture module, University of Westminster (IIT Sri Lanka)**
- **IIT ID:** 20232639
- **UOW ID:** w2120772

## Overview
This is the Smart Campus API built using JAX-RS (Jersey). It manages Rooms, Sensors, and historical Sensor Readings. The application runs entirely in memory and prevents data orphans, utilizing semantic HTTP error codes and global logging filters.

## How to Run in NetBeans & Tomcat 9
1. Open Apache NetBeans.
2. Go to **File > Open Project** and select the root directory containing `pom.xml`.
3. Right-click the project in the Projects pane and select **Clean and Build**.
4. Right-click the project and select **Run**.
5. NetBeans will automatically deploy the `.war` to your configured Apache Tomcat 9 server.
6. The Output pane should open showing the Tomcat deployment logs and the application startup.
7. The base API URL is now correctly configured to match your exact test requirements: `http://localhost:8080/smart-campus-api/api/v1`

---

## Part 1: Service Architecture & Setup

**Question 1: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.**

**Answer:**
The default lifecycle of a JAX-RS Resource class is "per-request." This means that the JAX-RS runtime instantiates a completely new instance of the resource class for every incoming HTTP request and destroys it after the response is sent. 

Because instances are constantly created and destroyed, any instance-level state (like standard `HashMap` or `ArrayList` fields) would be lost between requests. To maintain state across requests without a database, the data structures must be declared as `static` or managed by an external singleton service (like our `DataStore` class). Furthermore, because the server can handle multiple HTTP requests simultaneously, concurrent access to these shared `static` structures will occur. This necessitates the use of thread-safe collections, such as `ConcurrentHashMap`, to prevent race conditions, `ConcurrentModificationException`, and potential data loss or corruption when multiple clients try to create or modify data at the same time.

**Question 2: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?**

**Answer:**
HATEOAS (Hypermedia as the Engine of Application State) makes a REST API self-discoverable. By embedding hypermedia links (e.g., a `_links` object) inside API responses, the server dynamically informs the client of the actions and resources currently available based on the application's state.

This greatly benefits client developers by decoupling the client from rigid, hardcoded URL structures. Instead of relying solely on out-of-band static documentation—which can become outdated—the client navigates the API by following the provided links. If the backend changes its URL schemes or introduces new state transitions, the client can gracefully adapt as long as it knows which link relation (e.g., `rooms`, `sensors`) to follow, making the system highly resilient to change.

---

## Part 2: Room Management

**Question 1: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.**

**Answer:**
Returning **only IDs** (e.g., `["LIB-301", "LIB-302"]`) minimizes the payload size, which conserves network bandwidth and speeds up data transfer. However, this shifts a significant burden to the client: if the client needs to render the room details (name, capacity), it must make subsequent HTTP `GET` requests for each individual ID. This leads to the "N+1 query problem" over the network, heavily increasing overall latency and the number of HTTP connections.

Returning **full room objects** consumes more bandwidth per request, as the JSON payload is much larger. The main advantage is that the client receives all the necessary data to render the user interface immediately in a single round trip, reducing latency and simplifying client-side processing logic. A common middle-ground is returning a "summary" object containing the ID and the most critical fields, with a link to fetch the full details.

**Question 2: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.**

**Answer:**
Yes, the `DELETE` operation in this implementation is idempotent. Idempotency dictates that executing the same operation multiple times must yield the same overall system state as executing it just once.

If a client sends `DELETE /api/v1/rooms/LIB-301`, the server checks if the room exists. If it does (and has no active sensors), it is removed from the `DataStore`, and a `204 No Content` response is returned. If the client mistakenly sends the exact same request again, the server looks up the room, finds that it no longer exists, and returns a `404 Not Found`. Crucially, the internal state of the server remains identical to the state immediately after the first successful deletion. Because the server state does not change upon subsequent identical requests, the operation strictly adheres to the definition of idempotency.

---

## Part 3: Sensor Operations & Linking

**Question 1: We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?**

**Answer:**
When `@Consumes(MediaType.APPLICATION_JSON)` is explicitly declared, it restricts the resource method to only process requests where the `Content-Type` HTTP header indicates JSON.

If a client attempts to send data with a different `Content-Type`, such as `text/plain` or `application/xml`, the JAX-RS framework intercepts the request during the routing phase before the method is ever invoked. Since the runtime cannot find a method designed to consume the provided media type, it automatically rejects the request and returns a standard `415 Unsupported Media Type` HTTP status code to the client. This offloads content-negotiation validation from the developer to the framework.

**Question 2: You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?**

**Answer:**
The query parameter approach (`?type=CO2`) is superior for filtering because URL paths are intended to identify resources hierarchically. The path `/api/v1/sensors` correctly identifies the overarching collection of all sensors. Query parameters serve as modifiers to narrow down or transform the view of that specific collection without changing the resource identifier.

Conversely, making the filter a path variable (like `/api/v1/sensors/type/CO2`) creates rigid, artificial sub-resources that complicate the URL structure. It scales poorly: if we later need to filter by multiple attributes simultaneously (e.g., type and status), query parameters easily stack (`?type=CO2&status=ACTIVE`), whereas path variables would lead to deeply nested, combinatorial, and confusing endpoint mappings.

---

## Part 4: Deep Nesting with Sub-Resources

**Question 1: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?**

**Answer:**
The Sub-Resource Locator pattern strongly enforces the principle of Separation of Concerns (SoC) and promotes modularity. Rather than defining every deeply nested path inside a single, bloated `SensorResource` class, the locator dynamically delegates the request handling for `/readings` to a dedicated `SensorReadingResource`. 

This approach prevents the creation of a "God Class" anti-pattern in large APIs. It keeps individual resource classes small, cohesive, and focused exclusively on the logic of one specific entity. This dramatically improves readability, maintainability, and testability, as developers can isolate and test the `SensorReadingResource` independently of the overarching `SensorResource`.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging

**Question 2: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**

**Answer:**
A standard `404 Not Found` implies that the target URI itself (e.g., `POST /api/v1/sensors`) does not exist on the server. In our scenario, the target URI *does* exist, and the JSON payload syntax is perfectly well-formed, ruling out a generic `400 Bad Request`.

The `422 Unprocessable Entity` status code specifically indicates that the server understands the content type and the JSON syntax is correct, but the semantic instructions contained within the payload are invalid. A missing foreign key (e.g., referring to a non-existent `roomId`) is a classic semantic constraint violation, making `422` the most precise and descriptive status code for this exact failure state.

**Question 4: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?**

**Answer:**
Exposing raw Java stack traces constitutes a critical Information Disclosure vulnerability. Stack traces serve as a roadmap of the application's internal architecture. 

An attacker can extract highly specific information, including:
- The exact frameworks and versions being used (e.g., Jersey, Grizzly, specific JSON parsers).
- Internal package and class names, revealing the architectural design.
- The underlying operating system or filesystem structure (from file paths).

This information allows attackers to fingerprint the stack, cross-reference it with known CVEs (Common Vulnerabilities and Exposures), and craft highly targeted attacks (such as exploiting known deserialization flaws in specific library versions) rather than probing blindly.

**Question 5: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?**

**Answer:**
JAX-RS filters implement an Aspect-Oriented Programming (AOP) approach, allowing developers to intercept and process HTTP requests and responses globally.

If we manually insert `Logger.info()` into every resource method, we violate the DRY (Don't Repeat Yourself) principle, leading to massive code duplication and polluting the core business logic with infrastructure concerns. It also increases the risk of human error; a developer might simply forget to add logging to a newly created endpoint. Filters centralize this cross-cutting logic into a single, highly cohesive class. This guarantees consistent, system-wide observability and drastically simplifies maintenance.

---

## API Endpoints

<details>
<summary><strong>GET /api/v1/</strong> (Discovery)</summary>

Returns discovery information and links to root resources.

**Example Request:**
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/
```

**Example Response:**
```json
{
  "version": "1.0",
  "adminContact": "admin@smartcampus.edu",
  "description": "Smart Campus Sensor & Room Management API",
  "_links": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```
</details>

<details>
<summary><strong>GET /api/v1/rooms</strong> (Get All Rooms)</summary>

Retrieves a list of all rooms.

**Example Request:**
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
```
</details>

<details>
<summary><strong>POST /api/v1/rooms</strong> (Create a Room)</summary>

Creates a new room.

**Example Request:**
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "LIB-301", "name": "Main Library", "capacity": 150}'
```
</details>

<details>
<summary><strong>GET /api/v1/rooms/{roomId}</strong> (Get Room by ID)</summary>

Retrieves details for a specific room.

**Example Request:**
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```
</details>

<details>
<summary><strong>DELETE /api/v1/rooms/{roomId}</strong> (Delete a Room)</summary>

Deletes a room if it has no active sensors assigned.

**Example Request:**
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```
</details>

<details>
<summary><strong>POST /api/v1/sensors</strong> (Create a Sensor)</summary>

Creates a new sensor and assigns it to an existing room.

**Example Request:**
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "SENS-123", "roomId": "LIB-301", "type": "CO2", "status": "ACTIVE"}'
```
</details>

<details>
<summary><strong>GET /api/v1/sensors</strong> (Get All Sensors)</summary>

Retrieves all sensors. Supports filtering by type using the `?type=` query parameter.

**Example Request:**
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2
```
</details>

<details>
<summary><strong>GET /api/v1/sensors/{sensorId}</strong> (Get Sensor by ID)</summary>

Retrieves details for a specific sensor.

**Example Request:**
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/SENS-123
```
</details>

<details>
<summary><strong>GET /api/v1/sensors/{sensorId}/readings</strong> (Get Sensor Readings)</summary>

Retrieves all historical readings for a specific sensor.

**Example Request:**
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/SENS-123/readings
```
</details>

<details>
<summary><strong>POST /api/v1/sensors/{sensorId}/readings</strong> (Post Sensor Reading)</summary>

Adds a new reading to a specific sensor. The sensor must not be in `MAINTENANCE` mode.

**Example Request:**
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/SENS-123/readings \
  -H "Content-Type: application/json" \
  -d '{"id": "R-001", "value": 450.5, "timestamp": "2023-10-01T12:00:00Z"}'
```
</details>
