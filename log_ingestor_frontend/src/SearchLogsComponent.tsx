import React, { useState } from 'react';
import axios from 'axios';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { format } from 'date-fns';


interface Log {
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
const SearchLogsComponent: React.FC = () => {
  const [searchParam, setSearchParam] = useState({
    query: undefined as string | undefined,
    level: undefined as string | undefined,
    search_string: undefined as string | undefined,
    resource_id: undefined as string | undefined,
    fromTime: undefined as string | undefined,
    tillTime: undefined as string | undefined,
  });
  const [apiResponse, setApiResponse] = useState<Log[]>([]); // Assuming this state holds your API response


  const [fromTime, setFromTime] = useState<Date | null>(null);
  const [tillTime, setTillTime] = useState<Date | null>(null);

  const handleFromTimeChange = (date: Date | null) => {
    setFromTime(date);
    setSearchParam({...searchParam, fromTime:format(date!, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx")})
  };

  const handleTillTimeChange = (date: Date | null) => {
    setTillTime(date);
    setSearchParam({...searchParam, tillTime: format(date!, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx")})

  };

  const renderLogs = () => {
    return apiResponse.map((log, index) => (
      <div key={index} style={{ marginBottom: '20px', border: '1px solid #ccc', padding: '10px' }}>
        <strong>Level:</strong> {log.level}<br />
        <strong>Message:</strong> {log.message}<br />
        <strong>Resource ID:</strong> {log.resource_id}<br />
        <strong>Timestamp:</strong> {log.timestamp}<br />
        <strong>Trace ID:</strong> {log.trace_id}<br />
        <strong>Span ID:</strong> {log.span_id}<br />
        <strong>Commit:</strong> {log.commit}<br />
        <strong>Metadata:</strong> {log.metadata ? JSON.stringify(log.metadata) : 'N/A'}
      </div>
    ));
  };

  const handleSearch = async () => {
    try {
      const response = await axios.post('http://localhost:3000/api/filter', searchParam);
      setApiResponse(response.data)
      console.log(response.data); // Handle the response as needed
    } catch (error) {
      console.error('Error searching logs:', error);
    }
  };

  return (
    <div>
      <h1>Logs Explorer</h1>
      <div>
        <label>Query Type:</label>
        <select
          onChange={(e) =>
            setSearchParam({ ...searchParam, query: e.target.value })
          }
          value={searchParam.query || ""}
        >
          <option value="">Select Query Type</option>
          <option value="level">Level</option>
          <option value="message">Message</option>
          <option value="resource">Resource</option>
          <option value="timestamp">Timestamp</option>
        </select>
      </div>
      {searchParam.query === "level" && (
        <div>
          <label>Level:</label>
          <input
            type="text"
            value={searchParam.level}
            onChange={(e) =>
              setSearchParam({ ...searchParam, level: e.target.value })
            }
          />
        </div>
      )}
      {searchParam.query === "message" && (
        <div>
          <label>Search String:</label>
          <input
            type="text"
            value={searchParam.search_string}
            onChange={(e) =>
              setSearchParam({ ...searchParam, search_string: e.target.value })
            }
          />
        </div>
      )}
      {searchParam.query === "resource" && (
        <div>
          <label>Resource ID:</label>
          <input
            type="text"
            value={searchParam.resource_id}
            onChange={(e) =>
              setSearchParam({ ...searchParam, resource_id: e.target.value })
            }
          />
        </div>
      )}
      {searchParam.query === "timestamp" && (
        <div>
          <div>
            <label>From Time:</label>
            <DatePicker
              selected={fromTime}
              onChange={handleFromTimeChange}
              showTimeSelect
              dateFormat="Pp"
            />
          </div>
          <div>
            <label>Till Time:</label>
            <DatePicker
              selected={tillTime}
              onChange={handleTillTimeChange}
              showTimeSelect
              dateFormat="Pp"
            />
          </div>
        </div>
      )}
      <button onClick={handleSearch}>Search</button>

      <div>{renderLogs()}</div>
    </div>
  );
};

export default SearchLogsComponent;
