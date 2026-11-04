package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.renovai.api.dto.request.EmpresaContaRequests.Atualizar;
import com.renovai.api.repository.EmpresaSchemaRepository;
import com.renovai.api.security.JwtTokenProvider;
import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Persistência real em H2 isolado, usando o serviço e o SQL existentes da API. */
class PerfilEmpresaPersistenciaTest {
    private JdbcTemplate jdbc;
    private final UUID empresa = UUID.randomUUID(), perfil = UUID.randomUUID(), endereco = UUID.randomUUID();
    @BeforeEach void prepararBancoDeTeste() {
        JdbcDataSource data = new JdbcDataSource();
        data.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(data);
        jdbc.execute("create table usuarios(usuario_id uuid primary key,email varchar(255))");
        jdbc.execute("create table empresas(empresa_id uuid primary key,nome varchar(255),descricao text)");
        jdbc.execute("create table enderecos(endereco_id uuid primary key,logradouro varchar(255),cidade varchar(100))");
        jdbc.execute("create table perfis(perfil_id uuid primary key,empresa_id uuid,email varchar(255),cnpj varchar(30),endereco_id uuid,esta_ativo boolean)");
        jdbc.execute("create table telefones(telefone_id uuid primary key,perfil_id uuid,ddd varchar(4),telefone varchar(20))");
        jdbc.update("insert into empresas values(?,?,?)", empresa, "Empresa inicial", "Descrição inicial");
        jdbc.update("insert into enderecos values(?,?,?)", endereco, "Rua inicial", "Cidade inicial");
        jdbc.update("insert into perfis values(?,?,?,?,?,true)", perfil, empresa, "empresa@example.invalid", "11.222.333/0001-81", endereco);
        jdbc.update("insert into telefones values(?,?,?,?)", UUID.randomUUID(), perfil, "11", "999999999");
    }
    private EmpresaSchemaService abrirServico() {
        return new EmpresaSchemaService(new EmpresaSchemaRepository(jdbc), new BCryptPasswordEncoder(4), new JwtTokenProvider());
    }
    @Test void editarELerNovamentePreservaTodosOsCamposEIdentidade() {
        abrirServico().atualizar("empresa@example.invalid", new Atualizar(
                "Empresa atualizada", "novo@example.invalid", "11.222.333/0001-81", "Rua atualizada",
                "(11) 98888-7777", "Nova descrição", "São Paulo"));
        var lido = abrirServico().meuPerfil("novo@example.invalid");
        assertThat(lido.empresaId()).isEqualTo(empresa);
        assertThat(lido.perfilId()).isEqualTo(perfil);
        assertThat(lido.nomeEmpresa()).isEqualTo("Empresa atualizada");
        assertThat(lido.email()).isEqualTo("novo@example.invalid");
        assertThat(lido.cnpj()).isEqualTo("11.222.333/0001-81");
        assertThat(lido.endereco()).isEqualTo("Rua atualizada");
        assertThat(lido.telefone()).isEqualTo("(11) 98888-7777");
        assertThat(lido.descricao()).isEqualTo("Nova descrição");
        assertThat(lido.cidade()).isEqualTo("São Paulo");
    }
    @Test void limparOpcionaisNaoApagaOsDemaisDados() {
        abrirServico().atualizar("empresa@example.invalid", new Atualizar(null,null,null,null,null,"",""));
        var lido = abrirServico().meuPerfil("empresa@example.invalid");
        assertThat(lido.descricao()).isEmpty();
        assertThat(lido.cidade()).isEmpty();
        assertThat(lido.nomeEmpresa()).isEqualTo("Empresa inicial");
        assertThat(lido.telefone()).isEqualTo("11999999999");
    }
}
