import React, { useState } from 'react';
import './App.css';
import SearchLogsComponent from './SearchLogsComponent';
import IngestLogsComponent from './IngestLogsComponent';

type Tab = 'search' | 'ingest';

function App() {
  const [activeTab, setActiveTab] = useState<Tab>('search');

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <div className="brand">
            <span className="brand-icon">◉</span>
            <h1>Log Ingestor</h1>
          </div>
          <nav className="tab-nav">
            <button
              className={`tab-btn ${activeTab === 'search' ? 'active' : ''}`}
              onClick={() => setActiveTab('search')}
            >
              <span className="tab-icon">🔍</span> Search
            </button>
            <button
              className={`tab-btn ${activeTab === 'ingest' ? 'active' : ''}`}
              onClick={() => setActiveTab('ingest')}
            >
              <span className="tab-icon">➕</span> Ingest
            </button>
          </nav>
        </div>
      </header>

      <main className="app-main">
        {activeTab === 'search' && <SearchLogsComponent />}
        {activeTab === 'ingest' && <IngestLogsComponent />}
      </main>
    </div>
  );
}

export default App;
