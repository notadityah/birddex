// 
// Author: Riley Wade
// SignUpViewModel.swift (c) 2026
// Desc: Model for Sign Up View to call AuthService.swift and error handling
// Created:  2026-04-27 T06:33:38.305Z
// Modified: !date!
// 

import Foundation

@MainActor

final class SignUpViewModel: ObservableObject {

    //MARK: Form State 
    @Published var fullname:        String = ""
    @Published var email:           String = ""
    @Published var password:        String = ""
    @Published var confrimPassword: String = ""

    //MARK: UI State
    @Published var isLoading:       Bool = false
    @Published var errorMessage:    String? = nil
    @Published var showVerifyEmail: Bool = false

    //MARK: Sign Up 
    func signUp() async {
        errorMessage = nil
        isLoading = true
        defer {isLoading = false}

        do {
            try await AuthService.shared.signUp(
                name: fullname,
                email: email,
                password: password
            )
            //send verification email
            //navigate to confirmation screen
            showVerifyEmail = true 
        } catch AuthError.emailAlreadyInUse {
            errorMessage = "This email is already in use."
        } catch AuthError.weakPassword {
            errorMessage = "Password must be 8 or more characters."
        } catch AuthError.invalidEmail {
            errorMessage = "Please enter a valid email address."
        } catch APIError.networkUnavailable {
            errorMessage = "No internet connection. Please try again. "
        } catch {
            errorMessage = "Something went wrong. Please try again."
        }
    }

    //MARK: Field verification 

    var fullname: String? {
        guard !fullname.isEmpty else {return nil}
        return fullname.count >= 2 ? nil: "Please enter your full name."
    }

    var email: String? {
        guard !email.isEmpty else {return nil}
        return email.contains("@") ? nil : "Please enter a valid email."
    }

    var password: String? {
        guard !password.isEmpty else {return nil}
        return password.count >= 8 ? nil: "Password must be at least 8 characters."
    }

    var confrimPassword: String? {
        guard !confrimPassword.isEmpty else {return nil}
        return confrimPassword == password ? nil: "Passwords do not match."
    }

    var canSubmit: Bool {
        !fullname.isEmpty && !email.isEmpty &&
        !password.isEmpty && !confrimPassword.isEmpty &&
        !fullname = nil && !email = nil && !password = nil &&
        !confrimPassword = nil && isLoading = false
    }
}