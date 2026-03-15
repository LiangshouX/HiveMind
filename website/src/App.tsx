import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Chat from './pages/MorningCourt/Chat';
import EdictLibrary from './pages/MorningCourt/EdictLibrary';
import Channels from './pages/ImperialStudy/Channels';
import EdictBoard from './pages/ImperialStudy/EdictBoard';
import Memorials from './pages/ImperialStudy/Memorials';
import ScheduledTasks from './pages/ImperialStudy/ScheduledTasks';
import CourtRules from './pages/Censorate/CourtRules';
import SkillLibrary from './pages/Censorate/SkillLibrary';
import ToolLibrary from './pages/Censorate/ToolLibrary';
import MCP from './pages/Censorate/MCP';
import OfficialManagement from './pages/Censorate/OfficialManagement';
import Models from './pages/Dalisi/Models';
import EnvVars from './pages/Dalisi/EnvVars';
import Security from './pages/Dalisi/Security';
import TokenUsage from './pages/Dalisi/TokenUsage';

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/chat" replace />} />
          <Route path="chat" element={<Chat />} />
          <Route path="edict-library" element={<EdictLibrary />} />
          <Route path="channels" element={<Channels />} />
          <Route path="edict-board" element={<EdictBoard />} />
          <Route path="memorials" element={<Memorials />} />
          <Route path="scheduled-tasks" element={<ScheduledTasks />} />
          <Route path="court-rules" element={<CourtRules />} />
          <Route path="skill-library" element={<SkillLibrary />} />
          <Route path="tool-library" element={<ToolLibrary />} />
          <Route path="mcp" element={<MCP />} />
          <Route path="official-management" element={<OfficialManagement />} />
          <Route path="models" element={<Models />} />
          <Route path="env-vars" element={<EnvVars />} />
          <Route path="security" element={<Security />} />
          <Route path="token-usage" element={<TokenUsage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;
