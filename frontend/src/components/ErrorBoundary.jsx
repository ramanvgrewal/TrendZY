import React from 'react';

export default class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, info) {
        console.error('[TrendZY] Error caught by boundary:', error, info);
    }

    handleRetry = () => {
        this.setState({ hasError: false, error: null });
    };

    render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen flex items-center justify-center bg-bg px-4">
                    <div className="glass rounded-2xl p-10 max-w-md text-center">
                        <div className="text-5xl mb-4">⚠️</div>
                        <h2 className="font-[family-name:var(--font-heading)] text-2xl font-bold text-text-primary mb-3">
                            Something went wrong
                        </h2>
                        <p className="text-text-secondary mb-6 text-sm leading-relaxed">
                            TrendZY hit an unexpected error. This might be temporary — try again.
                        </p>
                        <button
                            onClick={this.handleRetry}
                            className="px-6 py-2.5 rounded-xl bg-accent text-bg font-semibold text-sm hover:opacity-90 transition-opacity cursor-pointer"
                        >
                            Retry
                        </button>
                    </div>
                </div>
            );
        }
        return this.props.children;
    }
}
