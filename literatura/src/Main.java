import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

// Classe para representar um livro
class Livro {
    String titulo;
    String autores;

    public Livro(String titulo, String autores) {
        this.titulo = titulo;
        this.autores = autores;
    }
}

// Cliente para consumir a API do Gutendex
class ClienteLivro {
    private final OkHttpClient httpClient = new OkHttpClient();

    public String buscarLivros(String consulta) throws Exception {
        Request request = new Request.Builder()
                .url("https://gutendex.com/books?search=" + consulta)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Código inesperado " + response);
            return response.body().string();
        }
    }
}

// Parser para analisar a resposta JSON da API
class ParserLivro {
    private final Gson gson = new Gson();

    public Livro[] parseLivros(String respostaJson) {
        JsonObject jsonObject = JsonParser.parseString(respostaJson).getAsJsonObject();
        JsonArray results = jsonObject.getAsJsonArray("results");

        Livro[] livros = new Livro[results.size()];
        for (int i = 0; i < results.size(); i++) {
            JsonObject bookInfo = results.get(i).getAsJsonObject();
            String titulo = bookInfo.get("title").getAsString();
            JsonArray authorsArray = bookInfo.getAsJsonArray("authors");
            StringBuilder autores = new StringBuilder();
            for (int j = 0; j < authorsArray.size(); j++) {
                if (j > 0) autores.append(", ");
                autores.append(authorsArray.get(j).getAsJsonObject().get("name").getAsString());
            }
            livros[i] = new Livro(titulo, autores.toString());
        }

        return livros;
    }
}

// Gerenciador do banco de dados SQLite
class GerenciadorBancoDados {
    private final String url = "jdbc:sqlite:livros.db";

    public GerenciadorBancoDados() {
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS livros (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "titulo TEXT NOT NULL," +
                    "autores TEXT" +
                    ");";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void inserirLivro(Livro livro) {
        String sql = "INSERT INTO livros(titulo, autores) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, livro.titulo);
            pstmt.setString(2, livro.autores);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void listarLivros() {
        String sql = "SELECT id, titulo, autores FROM livros";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Título: " + rs.getString("titulo"));
                System.out.println("Autores: " + rs.getString("autores"));
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void buscarLivrosPorAutor(String autor) {
        String sql = "SELECT id, titulo, autores FROM livros WHERE autores LIKE ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + autor + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Título: " + rs.getString("titulo"));
                System.out.println("Autores: " + rs.getString("autores"));
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void buscarLivrosPorTitulo(String titulo) {
        String sql = "SELECT id, titulo, autores FROM livros WHERE titulo LIKE ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + titulo + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Título: " + rs.getString("titulo"));
                System.out.println("Autores: " + rs.getString("autores"));
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

// Classe principal para interação via console
public class CatalogoLivrosApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ClienteLivro clienteLivro = new ClienteLivro();
        ParserLivro parserLivro = new ParserLivro();
        GerenciadorBancoDados dbManager = new GerenciadorBancoDados();

        while (true) {
            System.out.println("Menu:");
            System.out.println("1. Buscar Livro");
            System.out.println("2. Listar Livros no Catálogo");
            System.out.println("3. Buscar Livro por Autor");
            System.out.println("4. Buscar Livro por Título");
            System.out.println("5. Sair");

            int escolha = scanner.nextInt();
            scanner.nextLine(); // consumir a nova linha

            switch (escolha) {
                case 1:
                    System.out.println("Digite o nome do livro:");
                    String consulta = scanner.nextLine();
                    try {
                        String resposta = clienteLivro.buscarLivros(consulta);
                        Livro[] livros = parserLivro.parseLivros(resposta);
                        for (Livro livro : livros) {
                            dbManager.inserirLivro(livro);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    dbManager.listarLivros();
                    break;
                case 3:
                    System.out.println("Digite o nome do autor:");
                    String autor = scanner.nextLine();
                    dbManager.buscarLivrosPorAutor(autor);
                    break;
                case 4:
                    System.out.println("Digite o título do livro:");
                    String titulo = scanner.nextLine();
                    dbManager.buscarLivrosPorTitulo(titulo);
                    break;
                case 5:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }
}
