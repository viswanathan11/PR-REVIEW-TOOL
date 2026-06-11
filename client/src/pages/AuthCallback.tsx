import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

export default function AuthCallBack() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        //Read the token parameter from the URL: /auth/callback?token=xyz...

        const token = searchParams.get("token");

        if (token) {

            localStorage.setItem("token", token);


        }
    })
}