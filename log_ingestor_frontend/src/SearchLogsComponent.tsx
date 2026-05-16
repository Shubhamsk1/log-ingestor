import React, { useState } from 'react';
import axios from 'axios';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { format } from 'date-fns';
import { Log } from './types';

const levelColors: Record<string, string> = {
  error: '#ef4444',
  warn: '#f59e0b',
  info: '#3b82f6',
  debug: '#6b7280',
  trace: '#8b5cf6',
  fatal: '#dc2626',
};

const SearchLogsComponent: React.FC = () => {
  const [query, setQuery] = useState('');
  const [level, setLevel] = useState('');
  const [searchString, setSearchString] = useState('');
  const [resourceId, setResourceId] = useState('');
  const [fromTime, setFromTime] = useState<Date | null>(null);
  const [tillTime, setTillTime] = useState<Date | null>(null);
  const [results, setResults] = useState<Log[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  const buildPayload = () => {
    switch (query) {
      case 'level':
        return { query: 'level', level };
      case 'message':
        return { query: 'message', search_string: searchString };
      case 'resource':
        return { query: 'resource', resource_id: resourceId };
      case 'timestamp':
        return {
          query: 'timestamp',
          fromTime: fromTime ? format(fromTime, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx") : undefined,
          tillTime: tillTime ? format(tillTime, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx") : undefined,
        };
      default:
        return null;
    }
  };

  const handleSearch = async () => {
    const payload = buildPayload();
    if (!payload) return;
    setLoading(true);
    setError('');
    setSearched(true);
    try {
      const response = await axios.post('/api/filter', payload);
      setResults(response.data);
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message || 'Search failed');
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const formatTimestamp = (ts: string) => {
    try {
      return new Date(ts).toLocaleString();
    } catch {
      return ts;
    }
  };

  return (
    <div className="search-container">
      <h2 className="section-title">Search Logs</h2>
      <p className="section-desc">Filter and explore your log entries.</p>

      <div className="search-controls">
        <div className="query-selector">
          <label>Query Type</label>
          <select value={query} onChange={e => { setQuery(e.target.value); setSearched(false); }}>
            <option value="">Select query type</option>
            <option value="level">Level</option>
            <option value="message">Message</option>
            <option value="resource">Resource ID</option>
            <option value="timestamp">Timestamp Range</option>
          </select>
        </div>

        {query === 'level' && (
          <div className="filter-input">
            <label>Level</label>
            <select value={level} onChange={e => setLevel(e.target.value)}>
              <option value="">All levels</option>
              <option value="error">error</option>
              <option value="warn">warn</option>
              <option value="info">info</option>
              <option value="debug">debug</option>
              <option value="trace">trace</option>
              <option value="fatal">fatal</option>
            </select>
          </div>
        )}

        {query === 'message' && (
          <div className="filter-input">
            <label>Search String</label>
            <input type="text" value={searchString} onChange={e => setSearchString(e.target.value)} placeholder="Enter text to search..." />
          </div>
        )}

        {query === 'resource' && (
          <div className="filter-input">
            <label>Resource ID</label>
            <input type="text" value={resourceId} onChange={e => setResourceId(e.target.value)} placeholder="e.g. resource-123" />
          </div>
        )}

        {query === 'timestamp' && (
          <div className="filter-row">
            <div className="filter-input">
              <label>From</label>
              <DatePicker selected={fromTime} onChange={setFromTime} showTimeSelect dateFormat="Pp" placeholderText="From date" className="datepicker-input" />
            </div>
            <div className="filter-input">
              <label>Till</label>
              <DatePicker selected={tillTime} onChange={setTillTime} showTimeSelect dateFormat="Pp" placeholderText="Till date" className="datepicker-input" />
            </div>
          </div>
        )}

        <button className="btn btn-primary" onClick={handleSearch} disabled={loading || !query}>
          {loading ? <span className="spinner" /> : null}
          {loading ? 'Searching...' : 'Search'}
        </button>
      </div>

      {error && <div className="msg msg-error">{error}</div>}

      {loading && <div className="loading-indicator"><span className="spinner large" /> Searching logs...</div>}

      {!loading && searched && results.length === 0 && (
        <div className="empty-state">No logs found matching your query.</div>
      )}

      {!loading && results.length > 0 && (
        <div className="results-header">
          <span>{results.length} log{results.length !== 1 ? 's' : ''} found</span>
        </div>
      )}

      <div className="results-list">
        {results.map((log, i) => (
          <div key={i} className="log-card">
            <div className="log-card-header">
              <span className="log-level" style={{ backgroundColor: levelColors[log.level] || '#6b7280' }}>
                {log.level}
              </span>
              <span className="log-timestamp">{formatTimestamp(log.timestamp)}</span>
              <span className="log-resource">{log.resource_id}</span>
            </div>
            <div className="log-card-body">
              <p className="log-message">{log.message}</p>
              <div className="log-meta">
                <div className="meta-item"><span className="meta-label">Trace ID</span><span className="meta-value">{log.trace_id}</span></div>
                <div className="meta-item"><span className="meta-label">Span ID</span><span className="meta-value">{log.span_id}</span></div>
                <div className="meta-item"><span className="meta-label">Commit</span><span className="meta-value">{log.commit}</span></div>
                {log.metadata && (
                  <div className="meta-item"><span className="meta-label">Parent Resource</span><span className="meta-value">{log.metadata.parentResourceId}</span></div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SearchLogsComponent;
