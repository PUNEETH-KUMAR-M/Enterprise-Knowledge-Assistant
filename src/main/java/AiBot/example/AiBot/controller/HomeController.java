package AiBot.example.AiBot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>AiBot - Document Q&A System</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 15px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); text-align: center; }
                    h1 { color: #2c3e50; margin-bottom: 20px; font-size: 2.5em; }
                    p { color: #6c757d; margin-bottom: 30px; font-size: 1.1em; }
                    .btn { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: 500; transition: transform 0.2s; }
                    .btn:hover { transform: translateY(-2px); }
                    .features { margin: 30px 0; text-align: left; }
                    .feature { margin: 15px 0; padding: 10px; background: #f8f9fa; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🤖 AiBot</h1>
                    <p>Intelligent Document Q&A System with RAG Technology</p>
                    
                    <div class="features">
                        <div class="feature">📄 Upload PDF and text documents</div>
                        <div class="feature">🤖 AI-powered question answering using OpenAI</div>
                        <div class="feature">🔐 Secure authentication with JWT</div>
                        <div class="feature">👥 Role-based access (Admin/Employee)</div>
                    </div>
                    
                    <a href="/index.html" class="btn">🚀 Launch AiBot Interface</a>
                </div>
            </body>
            </html>
            """;
    }
}
