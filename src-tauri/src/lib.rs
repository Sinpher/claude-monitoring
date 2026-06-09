use tauri::Manager;

/// 启动 Tauri 应用，注册窗口和事件。
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            // 启动时显示悬浮窗
            if let Some(float_window) = app.get_webview_window("float") {
                let _ = float_window.show();
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
