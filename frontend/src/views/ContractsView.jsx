import React, { useState } from 'react';
import { FileText, ShieldAlert, Upload, Sparkles, Search, Database, FileSearch, CheckCircle } from 'lucide-react';
import { request, previewRagChunks } from '../services/api';
import { useAuth } from '../context/AuthContext';

export function ContractsView({ showToast }) {
  const { token, isAuthenticated } = useAuth();
  const [contractId, setContractId] = useState('1');
  const [documentText, setDocumentText] = useState('');
  const [riskReport, setRiskReport] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  // RAG Search State
  const [searchQuery, setSearchQuery] = useState('indemnification and liability');
  const [ragChunks, setRagChunks] = useState(previewRagChunks);
  const [isSearchingRag, setIsSearchingRag] = useState(false);

  const handleUploadText = async (e) => {
    e.preventDefault();
    if (!documentText.trim()) return;
    if (!isAuthenticated) return showToast('Connect workspace to upload contract text.', 'error');

    setIsUploading(true);
    try {
      await request(`/contracts/${contractId}/documents`, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: documentText
      }, token);

      showToast(`Contract #${contractId} text ingested and chunked for RAG.`);
      setDocumentText('');
    } catch (err) {
      showToast(err.message || 'Failed to upload contract text.', 'error');
    } finally {
      setIsUploading(false);
    }
  };

  const handleAnalyzeRisk = async () => {
    if (!isAuthenticated) return showToast('Connect workspace to analyze contract risk.', 'error');
    setIsAnalyzing(true);
    try {
      const res = await request(`/contracts/${contractId}/risk-analysis`, { method: 'POST' }, token);
      setRiskReport(res);
      showToast(`Risk analysis completed for Contract #${contractId}.`);
    } catch (err) {
      showToast(err.message || 'Failed to run risk analysis.', 'error');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleRagSearch = async (e) => {
    if (e) e.preventDefault();
    if (!searchQuery.trim()) return;

    if (!isAuthenticated) {
      // Filter preview chunks by query
      const q = searchQuery.toLowerCase();
      const filtered = previewRagChunks.filter(c => c.content.toLowerCase().includes(q));
      setRagChunks(filtered.length ? filtered : previewRagChunks);
      showToast(`RAG search evaluated in preview mode.`);
      return;
    }

    setIsSearchingRag(true);
    try {
      const res = await request(`/contracts/rag/search?query=${encodeURIComponent(searchQuery.trim())}&topK=5`, {}, token);
      setRagChunks(res || []);
      showToast(`Retrieved ${res?.length || 0} matching contract clauses via RAG.`);
    } catch (err) {
      showToast(err.message || 'RAG search failed.', 'error');
    } finally {
      setIsSearchingRag(false);
    }
  };

  return (
    <div className="contracts-view">
      {/* RAG Semantic Clause Explorer Section */}
      <div className="section-card" style={{ marginBottom: '24px' }}>
        <div className="section-header">
          <div className="section-title">
            <Sparkles size={20} />
            <span>RAG Semantic Clause Explorer</span>
          </div>
          <span style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600 }}>
            Hybrid BM25 + TF-IDF Vector Weighting
          </span>
        </div>

        <div className="section-body">
          <form onSubmit={handleRagSearch} style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
            <input
              type="text"
              className="analyst-input"
              placeholder="Search contract clauses e.g. indemnification, termination, automatic renewal, gdpr..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              required
            />
            <button className="btn-primary" type="submit" disabled={isSearchingRag}>
              <Search size={16} />
              <span>Search Clauses</span>
            </button>
          </form>

          {/* Preset Quick Queries */}
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' }}>
            <span style={{ fontSize: '12px', color: 'var(--muted)', alignSelf: 'center', fontWeight: 600 }}>Quick Queries:</span>
            {['indemnification and liability', 'automatic renewal terms', 'termination for convenience', 'data privacy gdpr', 'sla penalty'].map((preset, idx) => (
              <button
                key={idx}
                className="btn-secondary"
                style={{ padding: '4px 10px', fontSize: '11px' }}
                onClick={() => {
                  setSearchQuery(preset);
                  setTimeout(() => handleRagSearch(), 10);
                }}
              >
                {preset}
              </button>
            ))}
          </div>

          {/* RAG Results List */}
          <div style={{ display: 'grid', gap: '12px' }}>
            {ragChunks.map((chunk, idx) => {
              const scorePct = Math.round((chunk.score || 0) * 100);
              const isHigh = scorePct >= 70 || chunk.confidence === 'HIGH';
              return (
                <div key={idx} style={{ background: 'var(--canvas)', border: '1px solid var(--line)', borderRadius: '10px', padding: '16px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                      <span className="supplier-badge" style={{ width: 'auto', padding: '2px 8px', height: 'auto', fontSize: '11px' }}>
                        DOC #{chunk.sourceDocId || 1}
                      </span>
                      <span style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600 }}>
                        Chunk #{chunk.chunkId || idx + 1} ({chunk.sourceType || 'TEXT'})
                      </span>
                    </div>

                    <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                      <span className={`status-pill ${isHigh ? '' : 'pending'}`}>
                        <i className="pill-dot" />
                        {chunk.confidence || (isHigh ? 'HIGH' : 'MEDIUM')} CONFIDENCE
                      </span>
                      <strong className="score-badge" style={{ fontSize: '13px', color: 'var(--aqua-dark)' }}>
                        {scorePct}% Match
                      </strong>
                    </div>
                  </div>

                  <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink)', lineHeight: 1.55 }}>
                    "{chunk.content}"
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Document Ingestion & Risk Analysis Section */}
      <div className="section-card">
        <div className="section-header">
          <div className="section-title">
            <FileText size={20} />
            <span>Document Ingestion & RAG Risk Engine</span>
          </div>
        </div>

        <div className="section-body">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
            {/* Upload Document Text Form */}
            <div style={{ background: 'var(--canvas)', padding: '20px', borderRadius: '12px' }}>
              <h4 style={{ margin: '0 0 14px 0', fontSize: '15px', fontWeight: 800 }}>Ingest Contract Document</h4>
              <form onSubmit={handleUploadText}>
                <div className="form-group">
                  <label>Contract ID</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    value={contractId}
                    onChange={(e) => setContractId(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Contract Terms / Legal Clauses (Text Ingestion)</label>
                  <textarea 
                    className="form-control" 
                    rows={6}
                    placeholder="Paste master service agreement clauses, indemnification terms, penalty clauses..."
                    value={documentText}
                    onChange={(e) => setDocumentText(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: 'flex', gap: '10px' }}>
                  <button className="btn-primary" type="submit" disabled={isUploading}>
                    <Upload size={15} />
                    <span>Upload & Chunk Text</span>
                  </button>

                  <button className="btn-secondary" type="button" onClick={handleAnalyzeRisk} disabled={isAnalyzing}>
                    <Sparkles size={15} />
                    <span>Run AI Risk Analysis</span>
                  </button>
                </div>
              </form>
            </div>

            {/* Risk Report Output */}
            <div>
              <h4 style={{ margin: '0 0 14px 0', fontSize: '15px', fontWeight: 800 }}>Risk Analysis Report</h4>
              {riskReport ? (
                <div style={{ background: 'var(--paper)', border: '1px solid var(--line)', padding: '20px', borderRadius: '12px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                    <strong>Risk Level: <span style={{ color: 'var(--red)' }}>{riskReport.riskLevel || 'ELEVATED'}</span></strong>
                    <span className="risk-score elevated">Score: {riskReport.riskScore || 45}</span>
                  </div>

                  <p style={{ fontSize: '13px', color: 'var(--ink-soft)', lineHeight: 1.5 }}>
                    {riskReport.summary || 'Identified potential liability caps and missing termination for convenience clause.'}
                  </p>

                  {riskReport.findings && riskReport.findings.length > 0 && (
                    <div style={{ marginTop: '14px' }}>
                      <strong style={{ fontSize: '12px', color: 'var(--muted)' }}>FLAGGED CLAUSES:</strong>
                      <ul style={{ paddingLeft: '18px', fontSize: '12px', color: 'var(--ink)' }}>
                        {riskReport.findings.map((f, i) => (
                          <li key={i} style={{ margin: '4px 0' }}>{f.clauseType || f.summary || f}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              ) : (
                <div style={{ padding: '30px', textAlign: 'center', color: 'var(--muted)', fontSize: '13px', border: '1px dashed var(--line)', borderRadius: '12px' }}>
                  <ShieldAlert size={28} style={{ color: 'var(--muted)', marginBottom: '8px' }} />
                  <div>Select a Contract ID and run AI Risk Analysis to view clause flags and risk score.</div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

