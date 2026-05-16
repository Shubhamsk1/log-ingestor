import React, { useState } from 'react';
import axios from 'axios';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { format } from 'date-fns';

const emptyForm = {
  level: '',
  message: '',
  resourceId: '',
  timestamp: '',
  traceId: '',
  spanId: '',
  commit: '',
  parentResourceId: '',
};

const IngestLogsComponent: React.FC = () => {
  const [form, setForm] = useState(emptyForm);
  const [timestamp, setTimestamp] = useState<Date | null>(null);
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [statusMsg, setStatusMsg] = useState('');

  const update = (field: string, value: string) => setForm({ ...form, [field]: value });

  const handleTimestampChange = (date: Date | null) => {
    setTimestamp(date);
    update('timestamp', date ? format(date, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx") : '');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatus('loading');
    setStatusMsg('');
    try {
      const payload = [{
        level: form.level,
        message: form.message,
        resourceId: form.resourceId,
        timestamp: form.timestamp,
        traceId: form.traceId,
        spanId: form.spanId,
        commit: form.commit,
        ...(form.parentResourceId ? { metadata: { parentResourceId: form.parentResourceId } } : {}),
      }];
      await axios.post('/ingest', payload);
      setStatus('success');
      setStatusMsg('Log ingested successfully');
      setForm(emptyForm);
      setTimestamp(null);
    } catch (err: any) {
      setStatus('error');
      setStatusMsg(err?.response?.data?.message || err.message || 'Failed to ingest log');
    }
  };

  return (
    <div className="ingest-container">
      <h2 className="section-title">Ingest Log</h2>
      <p className="section-desc">Add a new log entry to the system.</p>

      <form onSubmit={handleSubmit} className="ingest-form">
        <div className="form-row">
          <div className="form-group">
            <label>Level *</label>
            <select value={form.level} onChange={e => update('level', e.target.value)} required>
              <option value="">Select level</option>
              <option value="error">error</option>
              <option value="warn">warn</option>
              <option value="info">info</option>
              <option value="debug">debug</option>
              <option value="trace">trace</option>
              <option value="fatal">fatal</option>
            </select>
          </div>
          <div className="form-group">
            <label>Resource ID *</label>
            <input type="text" value={form.resourceId} onChange={e => update('resourceId', e.target.value)} placeholder="e.g. resource-123" required />
          </div>
        </div>

        <div className="form-group">
          <label>Message *</label>
          <textarea value={form.message} onChange={e => update('message', e.target.value)} placeholder="Log message content" rows={3} required />
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Trace ID</label>
            <input type="text" value={form.traceId} onChange={e => update('traceId', e.target.value)} placeholder="trace-001" />
          </div>
          <div className="form-group">
            <label>Span ID</label>
            <input type="text" value={form.spanId} onChange={e => update('spanId', e.target.value)} placeholder="span-001" />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Commit</label>
            <input type="text" value={form.commit} onChange={e => update('commit', e.target.value)} placeholder="abc123" />
          </div>
          <div className="form-group">
            <label>Parent Resource ID</label>
            <input type="text" value={form.parentResourceId} onChange={e => update('parentResourceId', e.target.value)} placeholder="parent-abc" />
          </div>
        </div>

        <div className="form-group">
          <label>Timestamp *</label>
          <DatePicker
            selected={timestamp}
            onChange={handleTimestampChange}
            showTimeSelect
            dateFormat="Pp"
            placeholderText="Select date & time"
            required
            className="datepicker-input"
          />
        </div>

        <button type="submit" className="btn btn-primary" disabled={status === 'loading'}>
          {status === 'loading' ? <span className="spinner" /> : null}
          {status === 'loading' ? 'Ingesting...' : 'Ingest Log'}
        </button>

        {status === 'success' && <div className="msg msg-success">{statusMsg}</div>}
        {status === 'error' && <div className="msg msg-error">{statusMsg}</div>}
      </form>
    </div>
  );
};

export default IngestLogsComponent;
