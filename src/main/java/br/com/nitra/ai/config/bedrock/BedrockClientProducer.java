package br.com.nitra.ai.config.bedrock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.util.Optional;

@ApplicationScoped
public class BedrockClientProducer {

    @ConfigProperty(name = "aws.region", defaultValue = "us-east-2")
    String region;

    @ConfigProperty(name = "aws.accessKeyId")
    Optional<String> accessKeyId;

    @ConfigProperty(name = "aws.secretAccessKey")
    Optional<String> secretAccessKey;

    @Produces
    @ApplicationScoped
    public BedrockRuntimeClient bedrockClient() {
        var builder = BedrockRuntimeClient.builder()
                .region(Region.of(region));

        if (accessKeyId.isPresent() && !accessKeyId.get().isBlank()
                && secretAccessKey.isPresent() && !secretAccessKey.get().isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId.get(), secretAccessKey.get())
                    )
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
