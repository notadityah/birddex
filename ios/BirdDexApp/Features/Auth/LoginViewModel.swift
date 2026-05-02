// 
// Author: Riley Wade
// LoginViewModel.swift (c) 2026
// Desc: Model for ios login view
// Created:  2026-04-21 T02:21:25.416Z
// Modified: !date!
// 

import Foundation

@MainActor 

final class LoginViewModel: ObservableObject {

    //MARK: Form State
    @Published var email: String = ""
    @Published var password: String = ""

    //MARK: UI State
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var isLoggedIn: Bool = false
    @Published var showSignUp: Bool = false

    //MARK: Sign In
    func signIn() async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }

        do {
            try await AuthService.shared.signIn(email: email, password: password)
            isLoggedIn = true 
        } catch AuthError.invalidEmail {
            errorMessage = "Please enter a valid email address."
        } catch AuthError.weakPassword {
            errorMessage = "Password must be 8 or more characters."
        } catch APIError.networkUnavailable {
            errorMessage = "No internet connection, please try again."
        } catch APIError.unauthorized {
            errorMessage = "Invalid email or password."
        } catch {
            errorMessage = "Something went wrong. Please try again."
        }

    }

    //MARK: Validation

    var emailError: String? {
        guard !email.isEmpty else { nil }
        return email.contains("@") ? nil : "Please enter a valid email address."
    }

    var passwordError: String? {
        guard !password.isEmpty else { nil }
        return password.count >= 8? nil : "Password must be at least 8 characters."
    }

    var canSubmit: Bool {
        !email.isEmpty && !password.isEmpty &&
        emailError == nil && passwordError == nil &&
        !isLoading
    }

}
