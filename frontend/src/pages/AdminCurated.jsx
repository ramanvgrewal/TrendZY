import { useState, useEffect } from 'react';
import {
    fetchCuratedProducts,
    saveCuratedProduct,
    updateCuratedProduct,
    deleteCuratedProduct,
    bulkImportCurated
} from '../api/client';

const CATEGORIES = ['Streetwear', 'Sustainable', 'Denim', 'Indie', 'Handmade', 'Accessories', 'Beauty', 'Other'];
const VIBE_OPTIONS = [
    'Streetwear', 'Y2K', '90sVibes', 'Sustainable', 'Handmade',
    'IndieIndia', 'SmallBusiness', 'GenZ', 'BudgetFit', 'LuxuryPick', 'Limited'
];

const emptyForm = {
    category: 'Streetwear', productType: '', brandName: '', productName: '',
    imageUrl: '', websiteLink: '', priceInr: '', priceRange: '',
    description: '', sourceHandle: '', notes: '', vibeTags: [], featured: false
};

export default function AdminCurated() {
    const [authed, setAuthed] = useState(false);
    const [tab, setTab] = useState('add');
    const [form, setForm] = useState({ ...emptyForm });
    const [products, setProducts] = useState([]);
    const [toast, setToast] = useState('');
    const [editId, setEditId] = useState(null);
    const [showBulk, setShowBulk] = useState(false);
    const [bulkJson, setBulkJson] = useState('');
    const [bulkMsg, setBulkMsg] = useState('');

    // Auth check
    useEffect(() => {
        const stored = sessionStorage.getItem('trendzy_admin');
        if (stored === 'true') { setAuthed(true); return; }
        const pw = prompt('Enter admin password:');
        if (pw === 'trendzy2025') {
            sessionStorage.setItem('trendzy_admin', 'true');
            setAuthed(true);
        } else {
            setAuthed(false);
        }
    }, []);

    useEffect(() => {
        if (authed && tab === 'manage') loadProducts();
    }, [authed, tab]);

    const loadProducts = async () => {
        const data = await fetchCuratedProducts();
        setProducts(data);
    };

    const showToast = (msg) => {
        setToast(msg);
        setTimeout(() => setToast(''), 3000);
    };

    const handleChange = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
    };

    const toggleVibe = (vibe) => {
        setForm(prev => ({
            ...prev,
            vibeTags: prev.vibeTags.includes(vibe)
                ? prev.vibeTags.filter(v => v !== vibe)
                : [...prev.vibeTags, vibe]
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const payload = {
                ...form,
                priceInr: form.priceInr ? parseInt(form.priceInr) : null,
                isActive: true
            };
            if (editId) {
                await updateCuratedProduct(editId, payload);
                showToast('✅ Product updated!');
                setEditId(null);
            } else {
                await saveCuratedProduct(payload);
                showToast('✅ Added to ONLY ON TRENDZY!');
            }
            setForm({ ...emptyForm });
            if (tab === 'manage') loadProducts();
        } catch (err) {
            showToast('❌ Error: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleEdit = (p) => {
        setForm({
            category: p.category || 'Streetwear',
            productType: p.productType || '',
            brandName: p.brandName || '',
            productName: p.productName || '',
            imageUrl: p.imageUrl || '',
            websiteLink: p.websiteLink || '',
            priceInr: p.priceInr || '',
            priceRange: p.priceRange || '',
            description: p.description || '',
            sourceHandle: p.sourceHandle || '',
            notes: p.notes || '',
            vibeTags: p.vibeTags || [],
            featured: p.featured || false
        });
        setEditId(p.id);
        setTab('add');
    };

    const handleDelete = async (id) => {
        if (!confirm('Soft-delete this product?')) return;
        await deleteCuratedProduct(id);
        showToast('🗑️ Product deleted');
        loadProducts();
    };

    const handleBulkImport = async () => {
        try {
            const parsed = JSON.parse(bulkJson);
            if (!Array.isArray(parsed)) throw new Error('Must be a JSON array');
            const result = await bulkImportCurated(parsed);
            setBulkMsg(`✅ Successfully imported ${result.length} products`);
            setBulkJson('');
            loadProducts();
        } catch (err) {
            setBulkMsg('❌ ' + err.message);
        }
    };

    if (!authed) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-20 text-center">
                <span className="text-5xl block mb-4">🔒</span>
                <p className="font-body text-lg text-text">Access denied.</p>
                <button onClick={() => window.location.reload()}
                    className="mt-4 px-4 py-2 rounded-lg font-body text-sm"
                    style={{ background: 'var(--color-amber)', color: '#000' }}>
                    Retry
                </button>
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20 pt-8">
            {/* Toast */}
            {toast && (
                <div className="admin-toast">{toast}</div>
            )}

            {/* Header */}
            <div className="flex items-center gap-3 mb-8">
                <span style={{ color: 'var(--color-amber)', fontSize: 28 }}>✦</span>
                <h1 style={{ fontFamily: 'Playfair Display, serif', fontSize: 28, fontWeight: 800 }} className="text-text">
                    Admin — ONLY ON TRENDZY
                </h1>
            </div>

            {/* Tabs */}
            <div className="flex gap-2 mb-8">
                <button onClick={() => { setTab('add'); setEditId(null); setForm({ ...emptyForm }); }}
                    className={`admin-tab ${tab === 'add' ? 'active' : ''}`}>
                    ➕ Add Product
                </button>
                <button onClick={() => setTab('manage')}
                    className={`admin-tab ${tab === 'manage' ? 'active' : ''}`}>
                    📋 Manage Products
                </button>
                <button onClick={() => setShowBulk(true)} className="admin-tab" style={{ marginLeft: 'auto' }}>
                    📦 Bulk Import
                </button>
            </div>

            {/* Bulk Import Modal */}
            {showBulk && (
                <div className="admin-modal-overlay" onClick={() => setShowBulk(false)}>
                    <div className="admin-modal" onClick={e => e.stopPropagation()}>
                        <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 20, fontWeight: 700 }} className="text-text mb-4">
                            Bulk Import Products
                        </h3>
                        <p className="font-body text-sm text-text2 mb-3">
                            Paste a JSON array of products below:
                        </p>
                        <textarea
                            value={bulkJson}
                            onChange={e => setBulkJson(e.target.value)}
                            rows={10}
                            className="admin-input w-full font-mono text-xs"
                            placeholder='[{ "category": "Streetwear", "brandName": "...", ... }]'
                        />
                        {bulkMsg && <p className="font-body text-sm mt-2" style={{ color: bulkMsg.startsWith('✅') ? 'var(--color-lime)' : 'var(--color-red)' }}>{bulkMsg}</p>}
                        <div className="flex gap-2 mt-4">
                            <button onClick={handleBulkImport} className="admin-submit-btn flex-1">Import All</button>
                            <button onClick={() => { setShowBulk(false); setBulkMsg(''); }} className="admin-cancel-btn flex-1">Cancel</button>
                        </div>
                    </div>
                </div>
            )}

            {/* TAB: Add Product */}
            {tab === 'add' && (
                <form onSubmit={handleSubmit} className="admin-form">
                    <h2 className="admin-form-title">{editId ? 'Edit Product' : 'Add New Product'}</h2>

                    <div className="admin-form-grid">
                        {/* Category */}
                        <div className="admin-field">
                            <label className="admin-label">Category</label>
                            <select value={form.category} onChange={e => handleChange('category', e.target.value)} className="admin-input">
                                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                            </select>
                        </div>

                        {/* Product Type */}
                        <div className="admin-field">
                            <label className="admin-label">Product Type</label>
                            <input value={form.productType} onChange={e => handleChange('productType', e.target.value)}
                                className="admin-input" placeholder="e.g. T-Shirt" />
                        </div>

                        {/* Brand Name */}
                        <div className="admin-field">
                            <label className="admin-label">Brand Name *</label>
                            <input value={form.brandName} onChange={e => handleChange('brandName', e.target.value)}
                                className="admin-input" placeholder="e.g. WTF Wardrobe" required />
                        </div>

                        {/* Product Name */}
                        <div className="admin-field">
                            <label className="admin-label">Product Name *</label>
                            <input value={form.productName} onChange={e => handleChange('productName', e.target.value)}
                                className="admin-input" placeholder="e.g. Oversized Cotton Tee" required />
                        </div>

                        {/* Image URL */}
                        <div className="admin-field">
                            <label className="admin-label">Image URL</label>
                            <div className="flex items-center gap-3">
                                <input value={form.imageUrl} onChange={e => handleChange('imageUrl', e.target.value)}
                                    className="admin-input flex-1" placeholder="https://..." />
                                {form.imageUrl && (
                                    <img src={form.imageUrl} alt="preview" className="admin-img-preview"
                                        onError={e => e.target.style.display = 'none'} />
                                )}
                            </div>
                        </div>

                        {/* Website Link */}
                        <div className="admin-field">
                            <label className="admin-label">Website Link *</label>
                            <input value={form.websiteLink} onChange={e => handleChange('websiteLink', e.target.value)}
                                className="admin-input" placeholder="https://..." required />
                        </div>

                        {/* Price INR */}
                        <div className="admin-field">
                            <label className="admin-label">Price (INR)</label>
                            <input type="number" value={form.priceInr} onChange={e => handleChange('priceInr', e.target.value)}
                                className="admin-input" placeholder="e.g. 499" />
                        </div>

                        {/* Price Range */}
                        <div className="admin-field">
                            <label className="admin-label">Price Range</label>
                            <input value={form.priceRange} onChange={e => handleChange('priceRange', e.target.value)}
                                className="admin-input" placeholder="e.g. 400-700" />
                        </div>

                        {/* Description */}
                        <div className="admin-field" style={{ gridColumn: '1 / -1' }}>
                            <label className="admin-label">Description</label>
                            <textarea value={form.description} onChange={e => handleChange('description', e.target.value)}
                                className="admin-input" rows={3} placeholder="Short product description..." />
                        </div>

                        {/* Source Handle */}
                        <div className="admin-field">
                            <label className="admin-label">Source Handle</label>
                            <input value={form.sourceHandle} onChange={e => handleChange('sourceHandle', e.target.value)}
                                className="admin-input" placeholder="@wtfwardrobe" />
                        </div>

                        {/* Notes */}
                        <div className="admin-field">
                            <label className="admin-label">Notes</label>
                            <input value={form.notes} onChange={e => handleChange('notes', e.target.value)}
                                className="admin-input" placeholder="Internal notes" />
                        </div>
                    </div>

                    {/* Vibe Tags */}
                    <div className="admin-field mt-4">
                        <label className="admin-label">Vibe Tags</label>
                        <div className="flex flex-wrap gap-2 mt-1">
                            {VIBE_OPTIONS.map(vibe => (
                                <label key={vibe} className={`admin-vibe-check ${form.vibeTags.includes(vibe) ? 'active' : ''}`}>
                                    <input type="checkbox" checked={form.vibeTags.includes(vibe)}
                                        onChange={() => toggleVibe(vibe)} className="sr-only" />
                                    #{vibe}
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Featured toggle */}
                    <div className="admin-field mt-4">
                        <label className="admin-vibe-check" style={{ cursor: 'pointer' }}>
                            <input type="checkbox" checked={form.featured}
                                onChange={e => handleChange('featured', e.target.checked)} className="sr-only" />
                            <span className={`inline-block w-4 h-4 rounded border mr-2 ${form.featured ? 'bg-amber-500 border-amber-500' : 'border-gray-500'}`}
                                style={form.featured ? { background: 'var(--color-amber)', borderColor: 'var(--color-amber)' } : {}} />
                            ⭐ Featured (show on homepage)
                        </label>
                    </div>

                    {/* Submit */}
                    <button type="submit" className="admin-submit-btn mt-6 w-full">
                        {editId ? '💾 Update Product' : '✦ Add to ONLY ON TRENDZY'}
                    </button>
                    {editId && (
                        <button type="button" onClick={() => { setEditId(null); setForm({ ...emptyForm }); }}
                            className="admin-cancel-btn mt-2 w-full">
                            Cancel Edit
                        </button>
                    )}
                </form>
            )}

            {/* TAB: Manage Products */}
            {tab === 'manage' && (
                <div className="anim-fade-in">
                    <p className="font-body text-sm text-text2 mb-4">
                        Total products: <strong className="text-text">{products.length}</strong>
                    </p>
                    <div className="overflow-x-auto">
                        <table className="admin-table">
                            <thead>
                                <tr>
                                    <th>Image</th>
                                    <th>Brand</th>
                                    <th>Product Name</th>
                                    <th>Category</th>
                                    <th>Price</th>
                                    <th>Featured</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {products.map(p => (
                                    <tr key={p.id}>
                                        <td>
                                            {p.imageUrl ? (
                                                <img src={p.imageUrl} alt="" className="admin-table-img" />
                                            ) : (
                                                <div className="admin-table-img-placeholder">✦</div>
                                            )}
                                        </td>
                                        <td className="font-body text-sm text-text">{p.brandName}</td>
                                        <td className="font-body text-sm text-text">{p.productName}</td>
                                        <td className="font-body text-xs text-text2">{p.category}</td>
                                        <td className="font-body text-sm text-text">
                                            {p.priceInr ? `₹${p.priceInr}` : '—'}
                                        </td>
                                        <td>
                                            <span style={{ color: p.featured ? 'var(--color-amber)' : 'var(--color-text2)' }}>
                                                {p.featured ? '⭐' : '—'}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="flex gap-2">
                                                <button onClick={() => handleEdit(p)} className="admin-action-btn edit">Edit</button>
                                                <button onClick={() => handleDelete(p.id)} className="admin-action-btn delete">Delete</button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    {products.length === 0 && (
                        <div className="text-center py-12 text-text2 font-body">
                            No curated products yet. Add some from the "Add Product" tab!
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
