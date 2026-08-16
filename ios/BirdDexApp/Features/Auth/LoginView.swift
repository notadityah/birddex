// 
// Author: Riley Wade
// LoginView.swift (c) 2026
// Desc: login view including email and password inputs, communicates with AuthService for token handling 
// Created:  2026-04-21 T12:51:54.365Z
// Modified: !date!
// 

// Features/Auth/LoginView.swift

import SwiftUI

struct LoginView: View {

    @StateObject private var viewModel = LoginViewModel()
    @State private var showPassword    = false

    // Brand colours matching Vue's primary-green
    private let primaryGreen = Color(red: 0.02, green: 0.78, blue: 0.52)   
    private let darkGreen    = Color(red: 0.09, green: 0.22, blue: 0.18)  

    var body: some View {
        NavigationStack {
            ZStack {
                // Background — matches the subtle bird pattern bg in Vue
                Color(.systemBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 0) {

                        Spacer().frame(height: 60)

                        // MARK: Card (matches rounded-2xl shadow-xl p-8 in AuthLayout.vue)
                        VStack(spacing: 24) {

                            // MARK: Logo + heading
                            VStack(spacing: 12) {
                                // Green circle with logo — matches AuthLayout.vue
                                ZStack {
                                    Circle()
                                        .fill(primaryGreen)
                                        .frame(width: 48, height: 48)
                                    Image("logo")           // Assets.xcassets/logo
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 28, height: 28)
                                        .colorMultiply(.white)
                                }

                                VStack(spacing: 4) {
                                    Text("Welcome back")
                                        .font(.system(size: 24, weight: .bold))
                                        .foregroundStyle(Color(.label))

                                    Text("Sign in to your BirdDex account")
                                        .font(.system(size: 14))
                                        .foregroundStyle(Color(.secondaryLabel))
                                }
                            }

                            // MARK: Form fields (mirrors AuthFormInput.vue)
                            VStack(spacing: 16) {

                                // Email field
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("Email")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundStyle(Color(.label))

                                    TextField("you@example.com", text: $viewModel.email)
                                        .keyboardType(.emailAddress)
                                        .autocapitalization(.none)
                                        .autocorrectionDisabled()
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 10)
                                        .background(Color(.systemBackground))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 8)
                                                .stroke(
                                                    viewModel.emailError != nil
                                                        ? Color.red
                                                        : Color(.separator),
                                                    lineWidth: 1
                                                )
                                        )

                                    if let emailError = viewModel.emailError {
                                        Text(emailError)
                                            .font(.system(size: 12))
                                            .foregroundStyle(Color.red)
                                    }
                                }

                                // Password field
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("Password")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundStyle(Color(.label))

                                    HStack {
                                        Group {
                                            if showPassword {
                                                TextField("Enter your password", text: $viewModel.password)
                                            } else {
                                                SecureField("Enter your password", text: $viewModel.password)
                                            }
                                        }
                                        .autocapitalization(.none)
                                        .autocorrectionDisabled()

                                        // Eye toggle — mirrors AuthFormInput.vue
                                        Button {
                                            showPassword.toggle()
                                        } label: {
                                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                                .foregroundStyle(Color(.secondaryLabel))
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(Color(.systemBackground))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(
                                                viewModel.passwordError != nil
                                                    ? Color.red
                                                    : Color(.separator),
                                                lineWidth: 1
                                            )
                                    )

                                    // Forgot password — matches Vue positioning
                                    HStack {
                                        if let passwordError = viewModel.passwordError {
                                            Text(passwordError)
                                                .font(.system(size: 12))
                                                .foregroundStyle(Color.red)
                                        }
                                        Spacer()
                                        Button("Forgot password?") {
                                            // TODO: implement forgot password
                                        }
                                        .font(.system(size: 14))
                                        .foregroundStyle(primaryGreen)
                                    }
                                }
                            }

                            // MARK: Error banner
                            if let error = viewModel.errorMessage {
                                Text(error)
                                    .font(.system(size: 14))
                                    .foregroundStyle(Color.red)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }

                            // MARK: Sign in button (matches Vue dark green button)
                            Button {
                                Task { await viewModel.signIn() }
                            } label: {
                                Group {
                                    if viewModel.isLoading {
                                        ProgressView()
                                            .tint(.white)
                                    } else {
                                        Text("Sign In")
                                            .font(.system(size: 16, weight: .semibold))
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(viewModel.canSubmit ? darkGreen : Color(.systemGray4))
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                            .disabled(!viewModel.canSubmit)

                            // MARK: Divider + Google (mirrors SocialLoginButtons.vue)
                            // Google button shown but disabled until OAuth configured
                            VStack(spacing: 16) {
                                HStack {
                                    Rectangle()
                                        .fill(Color(.separator))
                                        .frame(height: 1)
                                    Text("or continue with")
                                        .font(.system(size: 14))
                                        .foregroundStyle(Color(.secondaryLabel))
                                        .fixedSize()
                                    Rectangle()
                                        .fill(Color(.separator))
                                        .frame(height: 1)
                                }

                                // Google button — disabled until VITE_ENABLE_GOOGLE_AUTH equivalent
                                Button {
                                    // TODO: implement Google OAuth via ASWebAuthenticationSession
                                } label: {
                                    HStack(spacing: 8) {
                                        // Google G logo using SF Symbol fallback
                                        // Replace with Image("google-logo") once asset is added
                                        Text("G")
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundStyle(Color.blue)
                                        Text("Google")
                                            .font(.system(size: 14, weight: .medium))
                                            .foregroundStyle(Color(.label))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(Color(.systemBackground))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(Color(.separator), lineWidth: 1)
                                    )
                                }
                                .disabled(true)   // enable when Google OAuth is configured
                                .opacity(0.5)
                            }

                            // MARK: Sign up link
                            HStack(spacing: 4) {
                                Text("Don't have an account?")
                                    .font(.system(size: 14))
                                    .foregroundStyle(Color(.secondaryLabel))
                                NavigationLink("Sign up", destination: SignUpView())
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(primaryGreen)
                            }
                        }
                        .padding(32)
                        .background(Color(.systemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .shadow(color: .black.opacity(0.08), radius: 24, x: 0, y: 8)
                        .padding(.horizontal, 24)

                        Spacer().frame(height: 60)
                    }
                }
            }
            // Navigate to main app on successful login
            .navigationDestination(isPresented: $viewModel.isLoggedIn) {
                CameraView()   // swap for your root app view
            }
        }
    }
}

//Preview 
{
    LoginView()
}