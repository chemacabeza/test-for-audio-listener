'use client';

import { TranscriptionResponse, getDownloadUrl } from '@/lib/api';
import Link from 'next/link';

interface TranscriptionListProps {
    transcriptions: TranscriptionResponse[];
    onDelete: (id: number) => void;
}

const STATUS_BADGES: Record<string, { label: string; className: string }> = {
    PENDING: { label: '⏳ Pending', className: 'badge badge-pending' },
    PROCESSING: { label: '⚙️ Processing', className: 'badge badge-processing' },
    COMPLETED: { label: '✅ Completed', className: 'badge badge-completed' },
    FAILED: { label: '❌ Failed', className: 'badge badge-failed' },
};

export default function TranscriptionList({ transcriptions, onDelete }: TranscriptionListProps) {
    if (transcriptions.length === 0) {
        return (
            <div className="empty-state">
                <p>🎧 No transcriptions yet. Record or upload audio to get started!</p>
            </div>
        );
    }

    const formatDate = (dateStr: string) => {
        return new Date(dateStr).toLocaleString();
    };

    const truncateText = (text: string | null, maxLength: number = 120) => {
        if (!text) return '—';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    };

    return (
        <div className="transcription-list">
            {transcriptions.map((t) => {
                const badge = STATUS_BADGES[t.status] || STATUS_BADGES.PENDING;
                return (
                    <div key={t.id} className="transcription-card">
                        <div className="transcription-header">
                            <Link href={`/transcriptions/${t.id}`} className="transcription-title">
                                {t.filename}
                            </Link>
                            <span className={badge.className}>{badge.label}</span>
                        </div>

                        <p className="transcription-preview">
                            {t.status === 'FAILED' ? t.errorMessage : truncateText(t.transcriptText)}
                        </p>

                        <div className="transcription-footer">
                            <span className="text-muted">{formatDate(t.createdAt)}</span>
                            {t.audioDurationSeconds && (
                                <span className="text-muted">⏱ {t.audioDurationSeconds.toFixed(1)}s</span>
                            )}
                            <div className="transcription-actions">
                                <Link href={`/transcriptions/${t.id}`} className="btn btn-sm">
                                    View
                                </Link>
                                <a
                                    href={getDownloadUrl(t.id)}
                                    className="btn btn-sm"
                                    download
                                    title="Download Original Audio"
                                >
                                    ⬇️
                                </a>
                                <button
                                    className="btn btn-sm btn-danger"
                                    onClick={(e) => {
                                        e.preventDefault();
                                        if (confirm('Delete this transcription?')) onDelete(t.id);
                                    }}
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
