'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { TranscriptionResponse, getTranscription, deleteTranscription, getDownloadUrl } from '@/lib/api';
import Link from 'next/link';

export default function TranscriptionDetailPage() {
    const params = useParams();
    const router = useRouter();
    const id = Number(params.id);

    const [transcription, setTranscription] = useState<TranscriptionResponse | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function load() {
            try {
                const data = await getTranscription(id);
                setTranscription(data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to load transcription');
            } finally {
                setIsLoading(false);
            }
        }
        if (id) load();
    }, [id]);

    const handleDelete = async () => {
        if (!confirm('Are you sure you want to delete this transcription?')) return;
        try {
            await deleteTranscription(id);
            router.push('/');
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Delete failed');
        }
    };

    if (isLoading) {
        return (
            <div className="container">
                <div className="loading-state"><div className="spinner" /> Loading...</div>
            </div>
        );
    }

    if (error || !transcription) {
        return (
            <div className="container">
                <div className="alert alert-error">{error || 'Transcription not found'}</div>
                <Link href="/" className="btn">← Back to Dashboard</Link>
            </div>
        );
    }

    const statusMap: Record<string, string> = {
        PENDING: '⏳ Pending',
        PROCESSING: '⚙️ Processing',
        COMPLETED: '✅ Completed',
        FAILED: '❌ Failed',
    };

    return (
        <div className="container">
            <Link href="/" className="back-link">← Back to Dashboard</Link>

            <div className="detail-card">
                <div className="detail-header">
                    <h1>{transcription.filename}</h1>
                    <span className={`badge badge-${transcription.status.toLowerCase()}`}>
                        {statusMap[transcription.status]}
                    </span>
                </div>

                <div className="detail-meta">
                    <div className="meta-item">
                        <span className="meta-label">File Type</span>
                        <span className="meta-value">{transcription.originalFileType || '—'}</span>
                    </div>
                    <div className="meta-item">
                        <span className="meta-label">Upload Date</span>
                        <span className="meta-value">{new Date(transcription.createdAt).toLocaleString()}</span>
                    </div>
                    <div className="meta-item">
                        <span className="meta-label">Last Updated</span>
                        <span className="meta-value">{new Date(transcription.updatedAt).toLocaleString()}</span>
                    </div>
                    {transcription.audioDurationSeconds && (
                        <div className="meta-item">
                            <span className="meta-label">Duration</span>
                            <span className="meta-value">{transcription.audioDurationSeconds.toFixed(1)}s</span>
                        </div>
                    )}
                </div>

                {transcription.status === 'COMPLETED' && transcription.transcriptText && (
                    <div className="transcript-section">
                        <h2>📝 Transcript</h2>
                        <div className="transcript-text">
                            {transcription.transcriptText}
                        </div>
                    </div>
                )}

                {transcription.status === 'FAILED' && transcription.errorMessage && (
                    <div className="error-section">
                        <h2>❌ Error Details</h2>
                        <pre className="error-details">{transcription.errorMessage}</pre>
                    </div>
                )}

                <div className="detail-actions">
                    <a href={getDownloadUrl(transcription.id)} className="btn btn-primary" download>
                        ⬇️ Download Original Audio
                    </a>
                    <button className="btn btn-danger" onClick={handleDelete}>
                        🗑 Delete Transcription
                    </button>
                </div>
            </div>
        </div>
    );
}
