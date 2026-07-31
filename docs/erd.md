# V1 Entity-Relationship Diagram

```mermaid
erDiagram
    CLIENTS ||--|| CONTRACTS : owns
    CONTRACT_TYPES ||--o{ REQUIREMENT_TEMPLATES : defines
    CONTRACT_TYPES ||--o{ CONTRACTS : classifies
    CONTRACTS ||--o{ CONTRACT_DEFINITION_ASSIGNMENTS : uses
    REQUIREMENT_TEMPLATES ||--o{ CONTRACT_DEFINITION_ASSIGNMENTS : assigned
    CONTRACTS ||--o{ CONTRACT_EVENTS : records
    CONTRACTS ||--o{ REQUIREMENTS : owns
    REQUIREMENT_TEMPLATES o|--o{ REQUIREMENTS : originates
    CONTRACTS ||--o{ REQUIREMENT_SUPPRESSIONS : suppresses
    CLIENTS ||--o{ NOTES : owns
    REQUIREMENTS o|--o{ NOTES : references
    REQUIREMENTS ||--o{ NOTIFICATION_DELIVERIES : receives
```

## Integrity rules

| Relationship or key | Database behavior |
| --- | --- |
| Client to contract | `contracts.client_id` is unique; a client has at most one contract row |
| Client ownership | Deleting a client cascades through its contract graph and notes |
| Definitions | Referenced contract types and templates use restricted deletion |
| Definition version | `(lineage_id, version)` is unique for types and templates |
| Occurrences | `occurrence_key` is unique and nullable for ad hoc work |
| Suppressions | `occurrence_key` is unique so a deleted generated occurrence stays deleted |
| Notes | Deleting a linked requirement sets `notes.requirement_id` to null |
| Deliveries | `(requirement_id, delivery_kind, delivery_date)` is unique |
| Dashboard access | Active flags, statuses, due dates, pinned flags, and update moments are indexed |

Public UUID strings are unique and are the only identifiers exposed by business APIs. Numeric keys
remain inside Room-backed implementations.
