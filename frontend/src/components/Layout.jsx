import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

export default function Layout() {
  const [searchTerm, setSearchTerm] = useState('');

  return (
    <div className="min-h-screen bg-background text-foreground">
      <Navbar searchTerm={searchTerm} onSearch={setSearchTerm} />
      <Outlet context={{ searchTerm }} />
    </div>
  );
}
