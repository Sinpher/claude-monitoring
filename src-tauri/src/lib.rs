use tauri::Manager;
use tauri::webview::Color;
use tauri_plugin_shell::ShellExt;

/// 启动 Tauri 应用，注册窗口和 SpringBoot 子进程管理。
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            // 显示悬浮窗并设置透明背景
            if let Some(float_window) = app.get_webview_window("float") {
                // Windows 上 WebView2 默认白色背景，需设为透明
                let _ = float_window.set_background_color(Some(Color(0, 0, 0, 0)));
                let _ = float_window.show();
            }

            // 启动 SpringBoot 后端子进程
            let shell = app.shell();
            let backend_jar = std::env::current_dir()
                .unwrap_or_default()
                .join("backend")
                .join("claude-monitoring-0.1.0.jar");

            // 开发模式下后端由用户手动启动
            // 生产模式下启动打包的 jar
            if backend_jar.exists() {
                let _ = shell.command("java")
                    .args(["-jar", backend_jar.to_str().unwrap_or("")])
                    .spawn();
            }

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
