# LogiFlow Permission Matrix

## Public
| Method | Endpoint | Permission |
|---|---|---|
| GET | `/api/logiflow/health` | Public |
| GET | `/v3/api-docs/**` | Public |
| GET | `/swagger-ui/**` | Public |
| GET | `/swagger-ui.html` | Public |

## Orders
| Method | Endpoint | Permission |
|---|---|---|
| POST | `/api/logiflow/orders` | `LOGIFLOW_ORDER_CREATE` |
| GET | `/api/logiflow/orders` | `LOGIFLOW_ORDER_VIEW` |
| GET | `/api/logiflow/orders/{id}` | `LOGIFLOW_ORDER_VIEW` |
| PATCH | `/api/logiflow/orders/{id}/status` | `LOGIFLOW_ORDER_UPDATE` |
| POST | `/api/logiflow/orders/{id}/assign` | `LOGIFLOW_ORDER_ASSIGN` |
| POST | `/api/logiflow/orders/{id}/tracking` | `LOGIFLOW_ORDER_TRACKING` |
| POST | `/api/logiflow/orders/{id}/cod` | `LOGIFLOW_COD_UPDATE` |

## Customers
| Method | Endpoint | Permission |
|---|---|---|
| POST | `/api/logiflow/customers` | `LOGIFLOW_CUSTOMER_CREATE` |
| GET | `/api/logiflow/customers` | `LOGIFLOW_CUSTOMER_VIEW` |
| GET | `/api/logiflow/customers/{id}` | `LOGIFLOW_CUSTOMER_VIEW` |
| PUT | `/api/logiflow/customers/{id}` | `LOGIFLOW_CUSTOMER_UPDATE` |
| DELETE | `/api/logiflow/customers/{id}` | `LOGIFLOW_CUSTOMER_DELETE` |

## Drivers
| Method | Endpoint | Permission |
|---|---|---|
| POST | `/api/logiflow/drivers` | `LOGIFLOW_DRIVER_CREATE` |
| GET | `/api/logiflow/drivers` | `LOGIFLOW_DRIVER_VIEW` |
| GET | `/api/logiflow/drivers/{id}` | `LOGIFLOW_DRIVER_VIEW` |
| PUT | `/api/logiflow/drivers/{id}` | `LOGIFLOW_DRIVER_UPDATE` |
| DELETE | `/api/logiflow/drivers/{id}` | `LOGIFLOW_DRIVER_DELETE` |

## Vehicles
| Method | Endpoint | Permission |
|---|---|---|
| POST | `/api/logiflow/vehicles` | `LOGIFLOW_VEHICLE_CREATE` |
| GET | `/api/logiflow/vehicles` | `LOGIFLOW_VEHICLE_VIEW` |
| GET | `/api/logiflow/vehicles/{id}` | `LOGIFLOW_VEHICLE_VIEW` |
| PUT | `/api/logiflow/vehicles/{id}` | `LOGIFLOW_VEHICLE_UPDATE` |
| DELETE | `/api/logiflow/vehicles/{id}` | `LOGIFLOW_VEHICLE_DELETE` |

## Reconciliation / Operations
| Method | Endpoint | Permission |
|---|---|---|
| GET | `/api/logiflow/operations/cod/summary` | `LOGIFLOW_COD_VIEW` |
| GET | `/api/logiflow/reconciliations/eligible-cod` | `LOGIFLOW_RECONCILIATION_VIEW` |
| POST | `/api/logiflow/reconciliations` | `LOGIFLOW_RECONCILIATION_CREATE` |
| GET | `/api/logiflow/reconciliations` | `LOGIFLOW_RECONCILIATION_VIEW` |
| GET | `/api/logiflow/reconciliations/{id}` | `LOGIFLOW_RECONCILIATION_VIEW` |
| PATCH | `/api/logiflow/reconciliations/{id}/status` | `LOGIFLOW_RECONCILIATION_UPDATE` |
