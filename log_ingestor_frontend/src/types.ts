export interface Log {
  level: string;
  message: string;
  resource_id: string;
  timestamp: string;
  trace_id: string;
  span_id: string;
  commit: string;
  metadata?: {
    parentResourceId: string;
  };
}

export interface IngestFormData {
  level: string;
  message: string;
  resourceId: string;
  timestamp: string;
  traceId: string;
  spanId: string;
  commit: string;
  parentResourceId: string;
}
