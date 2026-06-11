import { useEffect, useState } from "react";
import { FloatWindow } from "./components/float/FloatWindow";
import { Dashboard } from "./components/dashboard/Dashboard";

/**
 * 应用根组件，根据窗口标签决定渲染悬浮窗还是仪表盘。
 */
function App() {
  const [windowLabel, setWindowLabel] = useState<string>("main");

  useEffect(() => {
    // 从 URL 参数读取窗口标签
    const params = new URLSearchParams(window.location.search);
    setWindowLabel(params.get("label") || "main");
  }, []);

  if (windowLabel === "float") {
    return (
      <div
        className="w-full h-full"
        style={{ background: "transparent" }}
      >
        <FloatWindow />
      </div>
    );
  }

  return <Dashboard />;
}

export default App;
