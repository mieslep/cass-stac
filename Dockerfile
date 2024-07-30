# Use a Debian-based Java runtime as a parent image
FROM openjdk:17-jdk-slim

# Set environment variables
ENV JAVA_HOME=/usr/local/openjdk-17
ENV PATH=$JAVA_HOME/bin:$PATH
ENV SDKMAN_DIR="/root/.sdkman"
ENV PATH="$SDKMAN_DIR/bin:$PATH"
ENV TERM=xterm

# Install necessary packages including build tools
RUN apt-get update && apt-get install -y \
    apt-utils \
    curl \
    tar \
    git \
    python3 \
    python3-venv \
    python3-dev \
    maven \
    unzip \
    zip \
    build-essential \
    libssl-dev \
    zlib1g-dev \
    libbz2-dev \
    libreadline-dev \
    libsqlite3-dev \
    wget \
    llvm \
    libncurses5-dev \
    libncursesw5-dev \
    xz-utils \
    tk-dev \
    libffi-dev \
    liblzma-dev \
    && apt-get clean

# Install sdkman and Java using sdkman
RUN curl -s "https://get.sdkman.io" | bash && \
    bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install java 17.0.4.1-tem"

# Install Astra CLI
RUN curl -Ls "https://dtsx.io/get-astra-cli" | bash || true

# Install pyenv and set up Python environment
RUN curl https://pyenv.run | bash
ENV PATH="/root/.pyenv/bin:$PATH"
RUN echo 'export PATH="/root/.pyenv/bin:$PATH"' >> /root/.bashrc
RUN echo 'eval "$(pyenv init --path)"' >> /root/.bashrc
RUN echo 'eval "$(pyenv init -)"' >> /root/.bashrc
RUN bash -c "source /root/.bashrc && pyenv install 3.8.16 -f && pyenv global 3.8.16"

# Download and extract cqlsh-astra
RUN curl -O https://downloads.datastax.com/enterprise/cqlsh-astra-20221114-bin.tar.gz && \
    tar xvfz cqlsh-astra-20221114-bin.tar.gz && \
    mv cqlsh-astra /usr/local/bin

# Create and set the working directory
WORKDIR /app

# Clone the project repository and checkout the specified branch
RUN git clone -b feature/spring-restapi https://github.com/Anant/cass-stac.git /app

# Make setup script executable if it exists
RUN [ -f /app/setup.sh ] && chmod +x /app/setup.sh || echo "setup.sh not found"

# Run the setup script and package the Maven project if the script exists
RUN [ -f /app/setup.sh ] && /app/setup.sh || echo "setup.sh not found"
RUN mvn package -DskipTests=false

# Run the Spring Boot application with server port overridden
CMD ["sh", "-c", "mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=${SERVER_PORT}"]

