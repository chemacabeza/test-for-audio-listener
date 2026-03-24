'use client';

import { useState, useEffect, useCallback } from 'react';
import AudioRecorder from '@/components/AudioRecorder';
import FileUpload from '@/components/FileUpload';
import TranscriptionList from '@/components/TranscriptionList';
import {
  TranscriptionResponse,
  uploadAudio,
  getTranscriptions,
  deleteTranscription,
} from '@/lib/api';

export default function HomePage() {
  const [transcriptions, setTranscriptions] = useState<TranscriptionResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isOffline, setIsOffline] = useState(false);

  const fetchTranscriptions = useCallback(async (query?: string) => {
    setIsLoading(true);
    try {
      const data = await getTranscriptions(query);
      setTranscriptions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transcriptions');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTranscriptions();
  }, [fetchTranscriptions]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchTranscriptions(searchQuery || undefined);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery, fetchTranscriptions]);

  const handleAudioSubmit = async (file: File) => {
    setIsUploading(true);
    setError(null);
    setSuccessMsg(null);

    try {
      await uploadAudio(file, isOffline);
      setSuccessMsg('✅ Transcription completed successfully!');
      fetchTranscriptions(searchQuery || undefined);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteTranscription(id);
      setTranscriptions((prev: TranscriptionResponse[]) => prev.filter((t: TranscriptionResponse) => t.id !== id));
      setSuccessMsg('Transcription deleted');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  };

  // Auto-clear messages
  useEffect(() => {
    if (successMsg) {
      const timer = setTimeout(() => setSuccessMsg(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [successMsg]);

  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(null), 6000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  return (
    <div className="container">
      <header className="hero">
        <h1>🎧 Audio Listener</h1>
        <img
          src="/banner.png"
          alt="Audio Listener Banner"
          style={{
            display: 'block',
            margin: '20px auto 0',
            maxWidth: '100%',
            height: 'auto',
            borderRadius: '12px',
            boxShadow: '0 10px 30px rgba(0,0,0,0.3)'
          }}
        />
        <p>Record or upload audio and get instant transcriptions powered by OpenAI Whisper</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}
      {successMsg && <div className="alert alert-success">{successMsg}</div>}

      {isUploading && (
        <div className="alert alert-info">
          <div className="spinner" /> Transcribing audio... This may take a moment.
        </div>
      )}

      <div className="input-section" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {/* Toggle Switch */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px', background: 'rgba(255,255,255,0.05)', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span style={{ fontSize: '1.05rem', fontWeight: 600, color: isOffline ? '#a5b4fc' : '#e2e8f0', transition: '0.3s' }}>
              {isOffline ? '🔌 Offline Local Model' : '🌐 OpenAI Cloud API'}
            </span>
            <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.6)' }}>
              {isOffline ? 'Faster-Whisper on Local CPU (Free, Slower)' : 'OpenAI Whisper-1 (Ultra Fast, Very Accurate)'}
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => setIsOffline(!isOffline)}>
            <div style={{ position: 'relative', width: '52px', height: '28px', backgroundColor: isOffline ? 'rgba(99,120,255,0.7)' : 'rgba(255,255,255,0.2)', borderRadius: '34px', transition: '0.3s ease' }}>
              <div style={{ position: 'absolute', top: '4px', left: isOffline ? '28px' : '4px', width: '20px', height: '20px', backgroundColor: 'white', borderRadius: '50%', transition: '0.3s ease', boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }} />
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: '300px' }}>
            <AudioRecorder onRecordingComplete={handleAudioSubmit} disabled={isUploading} />
          </div>
          <div style={{ flex: 1, minWidth: '300px' }}>
            <FileUpload onFileSelected={handleAudioSubmit} disabled={isUploading} />
          </div>
        </div>
      </div>

      <section className="history-section">
        <div className="history-header">
          <h2>📋 Transcription History</h2>
          <input
            type="text"
            className="search-input"
            placeholder="🔍 Search transcriptions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {isLoading ? (
          <div className="loading-state">
            <div className="spinner" /> Loading transcriptions...
          </div>
        ) : (
          <TranscriptionList
            transcriptions={transcriptions}
            onDelete={handleDelete}
          />
        )}
      </section>
    </div>
  );
}
