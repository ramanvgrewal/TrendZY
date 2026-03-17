import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import Ticker from './Ticker';

export default function Layout() {
  const [searchTerm, setSearchTerm] = useState('');

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Ticker />
      <Navbar searchTerm={searchTerm} onSearch={setSearchTerm} />
      <main style={{ flex: 1 }}>
        <Outlet context={{ searchTerm }} />
      </main>
    </div>
  );
}
