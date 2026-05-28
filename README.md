# nitra-ai

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/nitra-ai-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and
  Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on
  it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus
  REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Amazon Bedrock ([guide](https://docs.quarkiverse.io/quarkus-amazon-services/dev/amazon-bedrock.html)): Connect to
  Amazon Bedrock service

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Deploy em EC2

Foi adicionada uma stack Terraform em `infra/terraform` para subir uma EC2 pequena, pública, com Amazon Linux 2023, SSM, Java 21 e uma role da instância para acessar o Bedrock e baixar artefatos do S3.

### Escolha da instância

O padrão da stack é `t4g.micro`.

- Ela continua barata e pequena.
- É mais realista para uma aplicação JVM do que `t4g.nano`, que tem apenas 0.5 GiB de RAM e tende a ficar apertada demais para Quarkus + systemd + SSM.
- Se você quiser forçar o menor tamanho absoluto, pode trocar `instance_type` no `terraform.tfvars`.

### Pré-requisitos

Você precisa de um backend remoto do Terraform já existente para o workflow aplicar mudanças com segurança:

- bucket S3 para o estado
- tabela DynamoDB para lock
- uma role AWS assumível via GitHub OIDC

### Aplicação manual inicial

Exemplo local usando o mesmo backend remoto do CI:

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init \
  -backend-config="bucket=SEU_BUCKET_DE_STATE" \
  -backend-config="key=SEU_CAMINHO_DE_STATE" \
  -backend-config="region=us-east-2" \
  -backend-config="dynamodb_table=SUA_TABELA_DE_LOCK"
terraform apply
```

### Secrets esperados no GitHub Actions

Crie estes secrets no repositório:

- `AWS_GITHUB_ROLE_ARN`
- `TF_STATE_BUCKET`
- `TF_STATE_KEY`
- `TF_STATE_LOCK_TABLE`

### Workflows

- `.github/workflows/infra.yml`
  - roda `fmt`, `validate` e `plan`
  - permite `apply` manual via `workflow_dispatch`
- `.github/workflows/deploy-ec2.yml`
  - roda testes
  - empacota o app em `uber-jar`
  - publica o artefato no bucket de deploy
  - usa AWS SSM para mandar a EC2 baixar o jar e reiniciar o serviço

### Acesso da aplicação

Depois do `terraform apply`, os outputs incluem:

- `service_url`
- `instance_public_ip`
- `instance_public_dns`

O serviço publica a aplicação diretamente na porta `8080` por padrão.

## Autenticação com Cognito

A API pode ser protegida com bearer token emitido por um Amazon Cognito User Pool usando o client `máquina-a-máquina`.

### Variáveis de ambiente

Configure estas variáveis para a aplicação:

- `COGNITO_ISSUER_URL`
- `COGNITO_CLIENT_ID`

Exemplo de issuer:

```text
https://cognito-idp.us-east-2.amazonaws.com/us-east-2_EXEMPLO
```

### Scopes usados pela API

Os endpoints foram protegidos com estes scopes:

- `POST /llm/chat` exige `nitra-ai/read`
- `POST /llm/chat/image` exige `nitra-ai/write`

No Quarkus, o claim `scope` do access token é mapeado para roles internas, o que permite proteger os endpoints com `@RolesAllowed`.
